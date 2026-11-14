package com.swiftdrop.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        Instant before = Instant.now();
        dispatcherService.dispatch(event);

        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository, atLeastOnce()).save(captor.capture());

        WebhookDelivery finalDelivery = captor.getValue();
        assertThat(finalDelivery.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(finalDelivery.getAttempts()).isEqualTo(1);
        assertThat(finalDelivery.getNextRetryAt()).isBetween(before.plusSeconds(8), Instant.now().plusSeconds(12));
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

    @Test
    @DisplayName("maskSecret should handle null, blank, short, and non-prefixed secrets")
    void shouldMaskSecretVariations() {
        assertThat(WebhookDispatcherService.maskSecret(null)).isNull();
        assertThat(WebhookDispatcherService.maskSecret("")).isEmpty();
        assertThat(WebhookDispatcherService.maskSecret("   ")).isEqualTo("   ");
        assertThat(WebhookDispatcherService.maskSecret("short")).isEqualTo("********");
        assertThat(WebhookDispatcherService.maskSecret("12345678")).isEqualTo("********");

        String withoutPrefix = "customsecret1234567890xyz";
        String maskedWithout = WebhookDispatcherService.maskSecret(withoutPrefix);
        assertThat(maskedWithout).endsWith("0xyz");
        assertThat(maskedWithout).contains("****");
    }

    @Test
    @DisplayName("Should skip dispatch when event is null or merchantId is null")
    void shouldSkipDispatchWhenEventOrMerchantIdIsNull() {
        dispatcherService.dispatch(null);

        ParcelStatusChangedEvent eventNoMerchant = new ParcelStatusChangedEvent(
                UUID.randomUUID(), 42L, "SD7K2M9QX4LP", "CREATED", "PICKED_UP",
                Instant.now(), "DISPATCHER", 1L, null, "Note"
        );
        dispatcherService.dispatch(eventNoMerchant);

        verify(endpointRepository, never()).findByMerchantIdAndActiveTrue(any());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip dispatch when no active endpoint is configured for merchant")
    void shouldSkipDispatchWhenNoActiveEndpoint() {
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(), 42L, "SD7K2M9QX4LP", "CREATED", "PICKED_UP",
                Instant.now(), "DISPATCHER", 1L, 999L, "Note"
        );
        when(endpointRepository.findByMerchantIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        dispatcherService.dispatch(event);

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("retryDelivery should abandon retry if endpoint is inactive or missing")
    void shouldAbandonRetryWhenEndpointInactiveOrMissing() {
        WebhookDelivery d1 = WebhookDelivery.builder().id(10L).endpoint(null).attempts(1).build();
        dispatcherService.retryDelivery(d1);
        assertThat(d1.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d1.getLastError()).isEqualTo("Endpoint is inactive or deleted");

        WebhookEndpoint inactiveEndpoint = WebhookEndpoint.builder().id(2L).active(false).build();
        WebhookDelivery d2 = WebhookDelivery.builder().id(11L).endpoint(inactiveEndpoint).attempts(1).build();
        dispatcherService.retryDelivery(d2);
        assertThat(d2.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(d2.getLastError()).isEqualTo("Endpoint is inactive or deleted");
    }

    @Test
    @DisplayName("resendDelivery should throw IllegalStateException if endpoint is inactive or missing")
    void shouldThrowIllegalStateExceptionOnResendWhenEndpointInactiveOrMissing() {
        WebhookDelivery d1 = WebhookDelivery.builder().id(10L).endpoint(null).build();
        assertThatThrownBy(() -> dispatcherService.resendDelivery(d1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot resend webhook: endpoint is inactive or missing");

        WebhookEndpoint inactiveEndpoint = WebhookEndpoint.builder().id(2L).active(false).build();
        WebhookDelivery d2 = WebhookDelivery.builder().id(11L).endpoint(inactiveEndpoint).build();
        assertThatThrownBy(() -> dispatcherService.resendDelivery(d2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot resend webhook: endpoint is inactive or missing");
    }

    @Test
    @DisplayName("resendDelivery(Long deliveryId) should look up delivery and execute resend")
    void shouldResendDeliveryByDeliveryId() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(50L)
                .endpoint(testEndpoint)
                .eventId("evt_50")
                .trackingNumber("SD7K2M9QX4LP")
                .payload("{\"test\":true}")
                .status(DeliveryStatus.FAILED)
                .attempts(5)
                .build();

        when(deliveryRepository.findById(50L)).thenReturn(Optional.of(delivery));
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        WebhookDeliveryDto result = dispatcherService.resendDelivery(50L);

        assertThat(result).isNotNull();
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.SUCCESS);
        assertThat(delivery.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("resendDelivery(Long deliveryId) should throw NotFoundException when delivery does not exist")
    void shouldThrowNotFoundExceptionOnResendNonExistentDelivery() {
        when(deliveryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispatcherService.resendDelivery(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Webhook delivery not found with ID: 999");
    }

    @Test
    @DisplayName("Should calculate correct backoff delays for attempts 1, 2, 3, and 4")
    void shouldCalculateCorrectBackoffDelays() {
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("sig");
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // Attempt 0 fails -> attempt become 1 -> delay is 10s
        Instant before0 = Instant.now();
        WebhookDelivery d0 = WebhookDelivery.builder().id(0L).endpoint(testEndpoint).payload("{}").attempts(0).build();
        dispatcherService.retryDelivery(d0);
        assertThat(d0.getAttempts()).isEqualTo(1);
        assertThat(d0.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d0.getNextRetryAt()).isBetween(before0.plusSeconds(8), Instant.now().plusSeconds(12));

        // Attempt 1 fails -> attempt become 2 -> delay is 30s
        Instant before1 = Instant.now();
        WebhookDelivery d1 = WebhookDelivery.builder().id(1L).endpoint(testEndpoint).payload("{}").attempts(1).build();
        dispatcherService.retryDelivery(d1);
        assertThat(d1.getAttempts()).isEqualTo(2);
        assertThat(d1.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d1.getNextRetryAt()).isBetween(before1.plusSeconds(28), Instant.now().plusSeconds(32));

        // Attempt 2 fails -> attempt become 3 -> delay is 120s
        Instant before2 = Instant.now();
        WebhookDelivery d2 = WebhookDelivery.builder().id(2L).endpoint(testEndpoint).payload("{}").attempts(2).build();
        dispatcherService.retryDelivery(d2);
        assertThat(d2.getAttempts()).isEqualTo(3);
        assertThat(d2.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d2.getNextRetryAt()).isBetween(before2.plusSeconds(115), Instant.now().plusSeconds(125));

        // Attempt 3 fails -> attempt become 4 -> delay is 120s
        Instant before3 = Instant.now();
        WebhookDelivery d3 = WebhookDelivery.builder().id(3L).endpoint(testEndpoint).payload("{}").attempts(3).build();
        dispatcherService.retryDelivery(d3);
        assertThat(d3.getAttempts()).isEqualTo(4);
        assertThat(d3.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d3.getNextRetryAt()).isBetween(before3.plusSeconds(115), Instant.now().plusSeconds(125));
    }

    @Test
    @DisplayName("Should handle ResourceAccessException and general Exception during delivery")
    void shouldHandleNetworkExceptionsDuringDelivery() {
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("sig");
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // ResourceAccessException
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        WebhookDelivery d1 = WebhookDelivery.builder().id(1L).endpoint(testEndpoint).payload("{}").attempts(0).build();
        dispatcherService.retryDelivery(d1);
        assertThat(d1.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d1.getLastError()).contains("Connection/Timeout error");

        // Generic Exception
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("SSL handshake failed"));

        WebhookDelivery d2 = WebhookDelivery.builder().id(2L).endpoint(testEndpoint).payload("{}").attempts(0).build();
        dispatcherService.retryDelivery(d2);
        assertThat(d2.getStatus()).isEqualTo(DeliveryStatus.RETRYING);
        assertThat(d2.getLastError()).contains("Unexpected error");
    }

    @Test
    @DisplayName("sendTestPing should throw NotFoundException when merchant has no configured endpoint")
    void shouldThrowNotFoundOnTestPingWhenEndpointMissing() {
        when(endpointRepository.findByMerchantId(888L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dispatcherService.sendTestPing(888L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("No webhook endpoint configured for merchantId: 888");
    }

    @Test
    @DisplayName("sendTestPing should return failed diagnosis on HTTP error or exception")
    void shouldHandleFailuresInTestPing() {
        when(endpointRepository.findByMerchantId(101L)).thenReturn(Optional.of(testEndpoint));
        when(webhookSigner.sign(anyString(), anyString())).thenReturn("sig");

        // Non-2xx without exception (returns non-2xx status entity)
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Bad Request", HttpStatus.BAD_REQUEST));

        TestPingResponse respNon2xx = dispatcherService.sendTestPing(101L);
        assertThat(respNon2xx.isSuccess()).isFalse();
        assertThat(respNon2xx.getHttpStatus()).isEqualTo(400);
        assertThat(respNon2xx.getMessage()).contains("Server returned non-2xx: 400");

        // HTTP error response (404)
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

        TestPingResponse resp1 = dispatcherService.sendTestPing(101L);
        assertThat(resp1.isSuccess()).isFalse();
        assertThat(resp1.getHttpStatus()).isEqualTo(404);
        assertThat(resp1.getMessage()).contains("Server error: 404");

        // General Exception (500)
        when(webhookRestTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        TestPingResponse resp2 = dispatcherService.sendTestPing(101L);
        assertThat(resp2.isSuccess()).isFalse();
        assertThat(resp2.getHttpStatus()).isEqualTo(500);
        assertThat(resp2.getMessage()).contains("Failed to reach endpoint");
    }

    @Test
    @DisplayName("toDto and toEndpointDto should accurately convert entities")
    void shouldConvertEntitiesToDtos() {
        WebhookDelivery delivery = WebhookDelivery.builder()
                .id(15L)
                .endpoint(testEndpoint)
                .eventId("evt_15")
                .trackingNumber("SD1234567890")
                .payload("{\"status\":\"OK\"}")
                .status(DeliveryStatus.SUCCESS)
                .attempts(1)
                .lastError(null)
                .lastAttemptAt(Instant.now())
                .nextRetryAt(null)
                .createdAt(Instant.now())
                .build();

        WebhookDeliveryDto dto = dispatcherService.toDto(delivery);
        assertThat(dto.getId()).isEqualTo(15L);
        assertThat(dto.getEndpointId()).isEqualTo(testEndpoint.getId());
        assertThat(dto.getTrackingNumber()).isEqualTo("SD1234567890");

        WebhookDelivery deliveryNullEndpoint = WebhookDelivery.builder()
                .id(16L)
                .endpoint(null)
                .build();
        WebhookDeliveryDto dtoNullEndpoint = dispatcherService.toDto(deliveryNullEndpoint);
        assertThat(dtoNullEndpoint.getEndpointId()).isNull();

        WebhookEndpointDto singleArgDto = dispatcherService.toEndpointDto(testEndpoint);
        assertThat(singleArgDto.getSecret()).contains("****");

        WebhookEndpointDto maskedDto = dispatcherService.toEndpointDto(testEndpoint, false);
        assertThat(maskedDto.getSecret()).contains("****");

        WebhookEndpointDto unmaskedDto = dispatcherService.toEndpointDto(testEndpoint, true);
        assertThat(unmaskedDto.getSecret()).isEqualTo(testEndpoint.getSecret());
    }

    @Test
    @DisplayName("dispatch should catch exceptions without propagating")
    void shouldCatchExceptionsInDispatch() {
        ParcelStatusChangedEvent event = new ParcelStatusChangedEvent(
                UUID.randomUUID(), 42L, "SD7K2M9QX4LP", "CREATED", "PICKED_UP",
                Instant.now(), "DISPATCHER", 1L, 101L, "Note"
        );
        when(endpointRepository.findByMerchantIdAndActiveTrue(101L)).thenReturn(Optional.of(testEndpoint));
        when(deliveryRepository.save(any(WebhookDelivery.class))).thenThrow(new RuntimeException("DB down"));

        // Should log and not throw
        dispatcherService.dispatch(event);
    }
}
