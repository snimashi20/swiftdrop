package com.swiftdrop.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.CoreApiApplication;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.ValidationException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.service.TrackingNumberGenerator;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.service.PricingService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = {CoreApiApplication.class, MerchantOrderPostgresIntegrationTest.Mocks.class})
class MerchantOrderPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("JWT_SECRET", () -> "integration-test-secret-integration-test-secret");
        registry.add("GOOGLE_CLIENT_ID", () -> "integration-test");
        registry.add("GOOGLE_CLIENT_SECRET", () -> "integration-test");
    }

    @Autowired PricingService pricingService;
    @Autowired TrackingNumberGenerator trackingNumberGenerator;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired MerchantOrderService orderService;
    @Autowired MerchantRepository merchantRepository;
    @Autowired MerchantOrderRepository orderRepository;
    @Autowired ParcelRepository parcelRepository;

    @TestConfiguration
    static class Mocks {
        @Bean @Primary PricingService pricingService() { return org.mockito.Mockito.mock(PricingService.class); }
        @Bean @Primary TrackingNumberGenerator trackingNumberGenerator() {
            return org.mockito.Mockito.mock(TrackingNumberGenerator.class);
        }
        @Bean @Primary RabbitTemplate rabbitTemplate() { return org.mockito.Mockito.mock(RabbitTemplate.class); }
    }

    private Long merchantId;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        Merchant merchant = merchantRepository.save(Merchant.builder()
                .businessName("Integration Merchant")
                .registrationNumber("REG-" + System.nanoTime())
                .contactName("Contact")
                .contactEmail("integration-" + System.nanoTime() + "@example.test")
                .contactPhone("0770000000")
                .status(MerchantStatus.APPROVED)
                .build());
        merchantId = merchant.getId();
        when(pricingService.calculateQuote(any())).thenReturn(QuoteResponse.builder()
                .fee(new BigDecimal("100.00")).pickupZoneId(1L).dropoffZoneId(1L).build());
        AtomicInteger sequence = new AtomicInteger();
        when(trackingNumberGenerator.generate()).thenAnswer(invocation ->
                sequence.getAndIncrement() == 0 ? "SDABCDEFGHIJ" : "SDKLMNOPQRS1");
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void concurrentSameKeyCreatesOneParcelAndPublishesOneEventAfterCommit() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Future<String> first = executor.submit(() -> createAtSameMoment(start));
        Future<String> second = executor.submit(() -> createAtSameMoment(start));

        assertEquals(first.get(), second.get());
        assertEquals(1, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "concurrent-key").stream().count());
        assertEquals(1, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
        verify(rabbitTemplate, timeout(5000).times(1))
                .convertAndSend(any(), any(), any(ParcelStatusChangedEvent.class));
    }

    @Test
    void failedAttemptLeavesNoOrphanParcelOrEvent() {
        when(pricingService.calculateQuote(any()))
                .thenThrow(new com.swiftdrop.common.exception.NotFoundException("Unknown district"));

        assertThrows(ValidationException.class,
                () -> orderService.create(merchantId, "failed-key", request()));

        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "failed-key").stream().count());
        assertEquals(0, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
        verify(rabbitTemplate, org.mockito.Mockito.never())
                .convertAndSend(any(), any(), any(ParcelStatusChangedEvent.class));
    }

    @Test
    void bulkFailureRollsBackAllParcelsAndOrders() {
        AtomicInteger quoteCalls = new AtomicInteger();
        when(pricingService.calculateQuote(any())).thenAnswer(invocation -> {
            if (quoteCalls.incrementAndGet() == 3) {
                throw new com.swiftdrop.common.exception.NotFoundException("Unknown district");
            }
            return QuoteResponse.builder().fee(new BigDecimal("100.00"))
                    .pickupZoneId(1L).dropoffZoneId(1L).build();
        });

        assertThrows(ValidationException.class,
                () -> orderService.createBulk(merchantId, "bulk-failure",
                        List.of(request(), request(), request())));

        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:0").stream().count());
        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:1").stream().count());
        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:2").stream().count());
        assertEquals(0, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
    }

    private String createAtSameMoment(CountDownLatch start) throws Exception {
        start.await();
        return orderService.create(merchantId, "concurrent-key", request())
                .response().getTrackingNumber();
    }

    private MerchantOrderRequest request() {
        MerchantOrderRequest request = new MerchantOrderRequest();
        var pickup = new com.swiftdrop.parcel.dto.ParcelAddressDto();
        pickup.setLine1("pickup");
        pickup.setCity("city");
        pickup.setDistrict("district");
        var dropoff = new com.swiftdrop.parcel.dto.ParcelAddressDto();
        dropoff.setLine1("dropoff");
        dropoff.setCity("city");
        dropoff.setDistrict("district");
        request.setPickup(pickup);
        request.setDropoff(dropoff);
        request.setReceiverName("receiver");
        request.setReceiverPhone("0771234567");
        request.setSize(ParcelSize.M);
        request.setWeightKg(BigDecimal.ONE);
        return request;
    }
}
