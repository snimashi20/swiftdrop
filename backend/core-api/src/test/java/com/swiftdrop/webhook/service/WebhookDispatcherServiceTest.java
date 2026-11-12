package com.swiftdrop.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import com.swiftdrop.webhook.repository.WebhookEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookDispatcherServiceTest {

    @Mock
    private WebhookEndpointRepository endpointRepository;

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @Mock
    private WebhookSigner webhookSigner;

    @Mock
    private RestTemplate webhookRestTemplate;

    private ObjectMapper objectMapper;
    private WebhookDispatcherService dispatcherService;

    private WebhookEndpoint testEndpoint;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        dispatcherService = new WebhookDispatcherService(
                endpointRepository,
                deliveryRepository,
                webhookSigner,
                webhookRestTemplate,
                objectMapper
        );

        testEndpoint = WebhookEndpoint.builder()
                .id(1L)
                .merchantId(101L)
                .url("http://localhost:8085/api/webhooks/orders")
                .secret("whsec_test_secret_12345")
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully dispatch webhook and mark status as SUCCESS")
    void shouldDispatchWebhookSuccessfully() {
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                42L,
                "SD7K2M9QX4LP",
                "PICKED_UP",
                "IN_TRANSIT",
                Instant.now(),
                "COURIER",
                10L,
                101L,
                "Departed facility"
        );

        when(endpointRepository.findByMerchantIdAndActiveTrue(101L)).thenReturn(Optional.of(testEndpoint));
        when(webhookSigner.sign(anyString(), eq(testEndpoint.getSecret()))).thenReturn("test_signature_hex");
        when(webhookRestTemplate.postForEntity(eq(testEndpoint.getUrl()), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcherService.dispatch(event);

        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository, atLeastOnce()).save(captor.capture());

        WebhookDelivery finalDelivery = captor.getValue();
        assertThat(finalDelivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(finalDelivery.getAttempts()).isEqualTo(1);
        assertThat(finalDelivery.getLastError()).isNull();
    }

    @Test
    @DisplayName("Should schedule backoff retry when HTTP endpoint returns 500")
    void shouldHandleFailureAndScheduleRetry() {
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                42L,
                "SD7K2M9QX4LP",
                "CREATED",
                "CANCELLED",
                Instant.now(),
                "CUSTOMER",
                10L,
                101L,
                "Order cancelled by customer"
        );

        when(endpointRepository.findByMerchantIdAndActiveTrue(101L)).thenReturn(Optional.of(testEndpoint));
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("test_sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR));

        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcherService.dispatch(event);

        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository, atLeastOnce()).save(captor.capture());

        WebhookDelivery finalDelivery = captor.getValue();
        assertThat(finalDelivery.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(finalDelivery.getAttempts()).isEqualTo(1);
        assertThat(finalDelivery.getNextRetryAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Should mark delivery as permanently FAILED when max attempts (5) are exhausted")
    void shouldMarkAsFailedWhenMaxAttemptsReached() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(99L)
                .endpoint(testEndpoint)
                .eventId("evt_123")
                .trackingNumber("SD7K2M9QX4LP")
                .payload("{\"test\": true}")
                .status(DeliveryStatus.RETRYING)
                .attempts(4) // This will become attempt 5
                .build();

        when(webhookSigner.sign(anyString(), anyString())).thenReturn("sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Service Unavailable", HttpStatus.SERVICE_UNAVAILABLE));

        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        dispatcherService.retryDelivery(delivery);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(delivery.getAttempts()).isEqualTo(5);
        assertThat(delivery.getNextRetryAt()).isNull();
    }

    @Test
    @DisplayName("Should send test diagnostic ping and return latency")
    void shouldSendTestPing() {
        when(endpointRepository.findByMerchantId(101L)).thenReturn(Optional.of(testEndpoint));
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("ping_sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"status\":\"pong\"}", HttpStatus.OK));

        TestPingResponse response = dispatcherService.sendTestPing(101L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getHttpStatus()).isEqualTo(200);
        assertThat(response.getDurationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("resendDelivery should reset attempts counter to 1 and execute fresh delivery attempt")
    void shouldResetAttemptsOnManualResend() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(77L)
                .endpoint(testEndpoint)
                .eventId("evt_resend_1")
                .trackingNumber("SD7K2M9QX4LP")
                .payload("{\"status\":\"DELIVERED\"}")
                .status(DeliveryStatus.FAILED)
                .attempts(5) // Previously exhausted attempts in DLQ
                .build();

        when(webhookSigner.sign(anyString(), anyString())).thenReturn("resend_sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookDeliveryDto result = dispatcherService.resendDelivery(delivery);

        assertThat(result).isNotNull();
        assertThat(delivery.getAttempts()).isEqualTo(1); // Reset to 1, does NOT increment past 5
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
    }

    @Test
    @DisplayName("maskSecret should mask secret while keeping whsec_ prefix and last 4 chars")
    void shouldMaskSecretProperly() {
        String original = "whsec_0123456789abcdef0123456789abcdef";
        String masked = WebhookDispatcherService.maskSecret(original);

        assertThat(masked).startsWith("whsec_");
        assertThat(masked).endsWith("cdef");
        assertThat(masked).contains("****");
        assertThat(masked).isNotEqualTo(original);
    }
}
