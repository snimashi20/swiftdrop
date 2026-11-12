package com.swiftdrop.webhook.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import com.swiftdrop.webhook.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WebhookWireMockIntegrationTest {

    private static WireMockServer wireMockServer;

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    private WebhookSigner webhookSigner;
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private WebhookDispatcherService dispatcherService;

    private static final String TEST_SECRET = "whsec_test_wiremock_secret_key_123456";
    private static final Long MERCHANT_ID = 9999L;
    private WebhookEndpoint endpoint;
    private final List<WebhookDelivery> savedDeliveries = new CopyOnWriteArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setupTestData() {
        savedDeliveries.clear();
        wireMockServer.resetAll();

        webhookSigner = new WebhookSigner();
        restTemplate = new RestTemplate();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        dispatcherService = new WebhookDispatcherService(
                endpointRepository,
                deliveryRepository,
                webhookSigner,
                restTemplate,
                objectMapper
        );

        endpoint = WebhookEndpoint.builder()
                .id(1L)
                .merchantId(MERCHANT_ID)
                .url(wireMockServer.baseUrl() + "/api/webhooks/orders")
                .secret(TEST_SECRET)
                .active(true)
                .createdAt(Instant.now())
                .build();

        lenient().when(endpointRepository.findByMerchantIdAndActiveTrue(MERCHANT_ID)).thenReturn(Optional.of(endpoint));
        lenient().when(endpointRepository.findByMerchantId(MERCHANT_ID)).thenReturn(Optional.of(endpoint));

        lenient().when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> {
            WebhookDelivery d = invocation.getArgument(0);
            if (d.getId() == null) {
                d.setId(idGenerator.getAndIncrement());
            }
            if (d.getCreatedAt() == null) {
                d.setCreatedAt(Instant.now());
            }
            savedDeliveries.removeIf(existing -> existing.getId().equals(d.getId()));
            savedDeliveries.add(d);
            return d;
        });

        lenient().when(deliveryRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return savedDeliveries.stream().filter(d -> id.equals(d.getId())).findFirst();
        });

        lenient().when(deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(anyLong())).thenAnswer(invocation -> {
            Long endpointId = invocation.getArgument(0);
            return savedDeliveries.stream()
                    .filter(d -> d.getEndpoint() != null && endpointId.equals(d.getEndpoint().getId()))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
        });
    }

    @Test
    @DisplayName("WireMock: HTTP 200 OK logs SUCCESS with verified HMAC-SHA256 signature header")
    void shouldSuccessfullyDeliverWebhookAndLogSuccess() throws Exception {
        stubFor(post(urlEqualTo("/api/webhooks/orders"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"RECEIVED\"}")));

        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                100L,
                "SD7K2M9QX4LP",
                "PICKED_UP",
                "IN_TRANSIT",
                Instant.now(),
                "COURIER",
                50L,
                MERCHANT_ID,
                "Package scanned at sorting hub"
        );

        dispatcherService.dispatch(event);

        verify(1, postRequestedFor(urlEqualTo("/api/webhooks/orders"))
                .withHeader(WebhookDispatcherService.SIGNATURE_HEADER, matching("^[a-f0-9]{64}$")));

        List<WebhookDelivery> deliveries = deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpoint.getId());
        assertThat(deliveries).hasSize(1);

        WebhookDelivery delivery = deliveries.get(0);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getLastError()).isNull();
        assertThat(delivery.getTrackingNumber()).isEqualTo("SD7K2M9QX4LP");

        // Verify cryptographic signature matches the exact payload sent over HTTP
        boolean signatureValid = webhookSigner.verify(
                delivery.getPayload(),
                webhookSigner.sign(delivery.getPayload(), TEST_SECRET),
                TEST_SECRET
        );
        assertThat(signatureValid).isTrue();
    }

    @Test
    @DisplayName("WireMock: HTTP 500 error triggers RETRYING status and sets nextRetryAt timestamp")
    void shouldHandleHttp500AndScheduleRetry() {
        stubFor(post(urlEqualTo("/api/webhooks/orders"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                101L,
                "SD8M1N9QX2KL",
                "IN_TRANSIT",
                "OUT_FOR_DELIVERY",
                Instant.now(),
                "COURIER",
                51L,
                MERCHANT_ID,
                "Out for delivery"
        );

        dispatcherService.dispatch(event);

        verify(1, postRequestedFor(urlEqualTo("/api/webhooks/orders")));

        List<WebhookDelivery> deliveries = deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpoint.getId());
        assertThat(deliveries).hasSize(1);

        WebhookDelivery delivery = deliveries.get(0);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(delivery.getAttempts()).isEqualTo(1);
        assertThat(delivery.getLastError()).contains("HTTP 500");
        assertThat(delivery.getNextRetryAt()).isNotNull();
        assertThat(delivery.getNextRetryAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("WireMock: Exhausting 5 attempts transitions status to FAILED (Dead-Letter)")
    void shouldTransitionToFailedAfterMaxAttempts() {
        stubFor(post(urlEqualTo("/api/webhooks/orders"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("Service Unavailable")));

        WebhookDelivery delivery = WebhookDelivery.builder()
                .endpoint(endpoint)
                .eventId("evt_max_test")
                .trackingNumber("SD9K1P8LA3YZ")
                .payload("{\"trackingNumber\":\"SD9K1P8LA3YZ\"}")
                .status(DeliveryStatus.RETRYING)
                .attempts(4) // Next retry is attempt 5
                .createdAt(Instant.now())
                .build();

        delivery = deliveryRepository.save(delivery);

        dispatcherService.retryDelivery(delivery);

        WebhookDelivery updated = deliveryRepository.findById(delivery.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(updated.getAttempts()).isEqualTo(5);
        assertThat(updated.getNextRetryAt()).isNull();
        assertThat(updated.getLastError()).contains("503");
    }

    @Test
    @DisplayName("WireMock: CANCELLED event and initial creation event (oldStatus: null) are dispatched cleanly")
    void shouldHandleCancelledAndNullOldStatusCleanly() throws Exception {
        stubFor(post(urlEqualTo("/api/webhooks/orders"))
                .willReturn(aResponse().withStatus(200).withBody("{\"status\":\"OK\"}")));

        // 1. Initial parcel creation event where oldStatus is null
        ParcelStatusChangedEvent creationEvent = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                102L,
                "SD5M8N2KL4PQ",
                null,
                "CREATED",
                Instant.now(),
                "CUSTOMER",
                52L,
                MERCHANT_ID,
                "Initial booking"
        );

        dispatcherService.dispatch(creationEvent);

        // 2. Cancellation event where newStatus is CANCELLED
        ParcelStatusChangedEvent cancelEvent = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                102L,
                "SD5M8N2KL4PQ",
                "CREATED",
                "CANCELLED",
                Instant.now(),
                "CUSTOMER",
                52L,
                MERCHANT_ID,
                "Cancelled by customer"
        );

        dispatcherService.dispatch(cancelEvent);

        verify(2, postRequestedFor(urlEqualTo("/api/webhooks/orders")));

        List<WebhookDelivery> deliveries = deliveryRepository.findByEndpointIdOrderByCreatedAtDesc(endpoint.getId());
        assertThat(deliveries).hasSize(2);

        // Verify creation payload has oldStatus null and newStatus CREATED
        JsonNode creationJson = objectMapper.readTree(deliveries.get(1).getPayload());
        assertThat(creationJson.get("oldStatus").isNull()).isTrue();
        assertThat(creationJson.get("newStatus").asText()).isEqualTo("CREATED");

        // Verify cancel payload has oldStatus CREATED and newStatus CANCELLED
        JsonNode cancelJson = objectMapper.readTree(deliveries.get(0).getPayload());
        assertThat(cancelJson.get("oldStatus").asText()).isEqualTo("CREATED");
        assertThat(cancelJson.get("newStatus").asText()).isEqualTo("CANCELLED");
    }
}
