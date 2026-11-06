package com.swiftdrop.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.CoreApiApplication;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.ValidationException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.domain.ApiKey;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.merchant.repository.ApiKeyRepository;
import com.swiftdrop.merchant.service.ApiKeyService;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.service.TrackingNumberGenerator;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = {CoreApiApplication.class, MerchantOrderPostgresIntegrationTest.Mocks.class})
@Timeout(value = 60, unit = TimeUnit.SECONDS)
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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "6");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "2");
    }

    @Autowired TrackingNumberGenerator trackingNumberGenerator;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired MerchantOrderService orderService;
    @Autowired MerchantRepository merchantRepository;
    @Autowired MerchantOrderRepository orderRepository;
    @Autowired ParcelRepository parcelRepository;
    @Autowired ApiKeyRepository apiKeyRepository;
    @Autowired ApiKeyService apiKeyService;
    @Autowired WebApplicationContext applicationContext;
    private MockMvc mockMvc;

    @TestConfiguration
    static class Mocks {
        @Bean @Primary TrackingNumberGenerator trackingNumberGenerator() {
            return org.mockito.Mockito.mock(TrackingNumberGenerator.class);
        }
        @Bean @Primary RabbitTemplate rabbitTemplate() { return org.mockito.Mockito.mock(RabbitTemplate.class); }
    }

    private Long merchantId;
    private String integrationApiKey;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        reset(rabbitTemplate, trackingNumberGenerator);
        orderRepository.deleteAll();
        parcelRepository.deleteAll();
        apiKeyRepository.deleteAll();
        merchantRepository.deleteAll();

        Merchant merchant = merchantRepository.save(Merchant.builder()
                .businessName("Integration Merchant")
                .registrationNumber("REG-" + System.nanoTime())
                .contactName("Contact")
                .contactEmail("integration-" + System.nanoTime() + "@example.test")
                .contactPhone("0770000000")
                .status(MerchantStatus.APPROVED)
                .build());
        merchantId = merchant.getId();
        integrationApiKey = "integration-test-api-key-" + System.nanoTime();
        apiKeyRepository.save(ApiKey.builder()
                .merchant(merchant)
                .label("Integration test key")
                .keyPrefix("sdk_test_key")
                .keyHash(apiKeyService.hashKey(integrationApiKey))
                .active(true)
                .build());
        AtomicInteger sequence = new AtomicInteger();
        when(trackingNumberGenerator.generate())
                .thenAnswer(invocation -> String.format("SD%010d", sequence.incrementAndGet()));
        executor = Executors.newFixedThreadPool(2);
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        reset(rabbitTemplate, trackingNumberGenerator);
        orderRepository.deleteAll();
        parcelRepository.deleteAll();
        apiKeyRepository.deleteAll();
        merchantRepository.deleteAll();
    }

    @Test
    void merchantApiUsesRealSecurityChainForValidApiKey() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-api/orders")
                        .header("X-API-Key", integrationApiKey)
                        .header("Idempotency-Key", "real-chain-key")
                        .contentType("application/json")
                        .content("""
                                {
                                  "pickup": {"line1": "12 Main St", "city": "Colombo", "district": "Colombo"},
                                  "dropoff": {"line1": "5 Lake Rd", "city": "Kandy", "district": "Kandy"},
                                  "receiverName": "Test Receiver",
                                  "receiverPhone": "0771234567",
                                  "size": "S",
                                  "weightKg": 1.5,
                                  "externalOrderId": "REAL-CHAIN-1001"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void merchantApiReturnsJsonForEmptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-api/orders")
                        .header("X-API-Key", integrationApiKey)
                        .header("Idempotency-Key", "empty-body-key")
                        .contentType("application/json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void concurrentSameKeyCreatesOneParcelAndPublishesOneEventAfterCommit() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<String> first = executor.submit(() -> createAtSameMoment(ready, start));
        Future<String> second = executor.submit(() -> createAtSameMoment(ready, start));

        assertTrue(ready.await(10, TimeUnit.SECONDS), "Concurrent workers did not become ready");
        start.countDown();
        String firstTracking = first.get(30, TimeUnit.SECONDS);
        String secondTracking = second.get(30, TimeUnit.SECONDS);
        assertEquals(firstTracking, secondTracking);
        assertEquals(1, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "concurrent-key").stream().count());
        assertEquals(1, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
        verify(rabbitTemplate, timeout(5000).times(1))
                .convertAndSend(any(), any(), any(ParcelStatusChangedEvent.class));
    }

    @Test
    void failedAttemptLeavesNoOrphanParcelOrEvent() {
        assertThrows(ValidationException.class,
                () -> orderService.create(merchantId, "failed-key", request("Unknown District", "Kandy")));

        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "failed-key").stream().count());
        assertEquals(0, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
        verify(rabbitTemplate, org.mockito.Mockito.never())
                .convertAndSend(any(), any(), any(ParcelStatusChangedEvent.class));
    }

    @Test
    void bulkFailureRollsBackAllParcelsAndOrders() {
        assertThrows(ValidationException.class,
                () -> orderService.createBulk(merchantId, "bulk-failure",
                        List.of(request(), request(), request("Unknown District", "Kandy"))));

        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:0").stream().count());
        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:1").stream().count());
        assertEquals(0, orderRepository.findByMerchantIdAndIdempotencyKey(
                merchantId, "bulk-failure:2").stream().count());
        assertEquals(0, parcelRepository.findAll().stream()
                .filter(parcel -> merchantId.equals(parcel.getMerchantId())).count());
    }

    private String createAtSameMoment(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        try {
            if (!start.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent test did not release its start latch");
            }
            return orderService.create(merchantId, "concurrent-key", request())
                    .response().getTrackingNumber();
        } catch (Throwable failure) {
            start.countDown();
            throw failure;
        }
    }

    private MerchantOrderRequest request() {
        return request("Colombo", "Kandy");
    }

    private MerchantOrderRequest request(String pickupDistrict, String dropoffDistrict) {
        MerchantOrderRequest request = new MerchantOrderRequest();
        var pickup = new com.swiftdrop.parcel.dto.ParcelAddressDto();
        pickup.setLine1("pickup");
        pickup.setCity("city");
        pickup.setDistrict(pickupDistrict);
        var dropoff = new com.swiftdrop.parcel.dto.ParcelAddressDto();
        dropoff.setLine1("dropoff");
        dropoff.setCity("city");
        dropoff.setDistrict(dropoffDistrict);
        request.setPickup(pickup);
        request.setDropoff(dropoff);
        request.setReceiverName("receiver");
        request.setReceiverPhone("0771234567");
        request.setSize(ParcelSize.M);
        request.setWeightKg(BigDecimal.ONE);
        return request;
    }
}
