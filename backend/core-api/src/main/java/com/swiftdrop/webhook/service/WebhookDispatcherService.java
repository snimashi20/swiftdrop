package com.swiftdrop.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.domain.WebhookDelivery;
import com.swiftdrop.webhook.domain.WebhookEndpoint;
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
import com.swiftdrop.webhook.dto.WebhookPayload;
import com.swiftdrop.webhook.repository.WebhookDeliveryRepository;
import com.swiftdrop.webhook.repository.WebhookEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WebhookDispatcherService {

    public static final String SIGNATURE_HEADER = "X-SwiftDrop-Signature";
    public static final int MAX_ATTEMPTS = 5;

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookSigner webhookSigner;
    private final RestTemplate webhookRestTemplate;
    private final ObjectMapper objectMapper;

    public WebhookDispatcherService(WebhookEndpointRepository endpointRepository,
                                  WebhookDeliveryRepository deliveryRepository,
                                  WebhookSigner webhookSigner,
                                  RestTemplate webhookRestTemplate,
                                  ObjectMapper objectMapper) {
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookSigner = webhookSigner;
        this.webhookRestTemplate = webhookRestTemplate;
        ObjectMapper mapper = (objectMapper != null) ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        this.objectMapper = mapper;
    }

    /**
     * Dispatches a webhook for an incoming parcel status changed event.
     */
    @Transactional
    public void dispatch(ParcelStatusChangedEvent event) {
        if (event == null || event.merchantId() == null) {
            log.debug("Skipping webhook dispatch: event is null or not associated with a merchant");
            return;
        }

        WebhookEndpoint endpoint = endpointRepository.findByMerchantIdAndActiveTrue(event.merchantId())
                .orElse(null);

        if (endpoint == null) {
            log.debug("No active webhook endpoint registered for merchantId: {}", event.merchantId());
            return;
        }

        try {
            WebhookPayload payload = WebhookPayload.builder()
                    .eventId(event.eventId() != null ? event.eventId().toString() : UUID.randomUUID().toString())
                    .parcelId(event.parcelId())
                    .trackingNumber(event.trackingNumber())
                    .oldStatus(event.oldStatus()) // safely supports null on parcel creation
                    .newStatus(event.newStatus()) // supports CANCELLED, DELIVERED, etc.
                    .changedAt(event.changedAt() != null ? event.changedAt() : Instant.now())
                    .changedByRole(event.changedByRole())
                    .note(event.note())
                    .build();

            String rawPayload = objectMapper.writeValueAsString(payload);

            WebhookDelivery delivery = WebhookDelivery.builder()
                    .endpoint(endpoint)
                    .eventId(payload.getEventId())
                    .trackingNumber(payload.getTrackingNumber())
                    .payload(rawPayload)
                    .status(DeliveryStatus.PENDING)
                    .attempts(1)
                    .createdAt(Instant.now())
                    .build();

            delivery = deliveryRepository.save(delivery);

            executeDeliveryAttempt(delivery, endpoint, rawPayload);

        } catch (Exception e) {
            log.error("Failed to construct or dispatch webhook for trackingNumber {}: {}", event.trackingNumber(), e.getMessage(), e);
        }
    }

    /**
     * Retries a pending delivery according to exponential backoff schedule.
     */
    @Transactional
    public void retryDelivery(WebhookDelivery delivery) {
        WebhookEndpoint endpoint = delivery.getEndpoint();
        if (endpoint == null || !Boolean.TRUE.equals(endpoint.getActive())) {
            log.warn("Abandoning retry for deliveryId {}: endpoint is inactive or missing", delivery.getId());
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setLastError("Endpoint is inactive or deleted");
            deliveryRepository.save(delivery);
            return;
        }

        delivery.setAttempts(delivery.getAttempts() + 1);
        executeDeliveryAttempt(delivery, endpoint, delivery.getPayload());
    }

    /**
     * Executes the HTTP POST request and updates delivery record accordingly.
     */
    private void executeDeliveryAttempt(WebhookDelivery delivery, WebhookEndpoint endpoint, String rawPayload) {
        String signature = webhookSigner.sign(rawPayload, endpoint.getSecret());
        Instant now = Instant.now();
        delivery.setLastAttemptAt(now);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(SIGNATURE_HEADER, signature);
        headers.set(HttpHeaders.USER_AGENT, "SwiftDrop-Webhook-Engine/1.0");

        HttpEntity<String> requestEntity = new HttpEntity<>(rawPayload, headers);

        try {
            log.info("Sending webhook to {} for tracking {} (Attempt {}/{})",
                    endpoint.getUrl(), delivery.getTrackingNumber(), delivery.getAttempts(), MAX_ATTEMPTS);

            ResponseEntity<String> response = webhookRestTemplate.postForEntity(endpoint.getUrl(), requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook delivered successfully to {} (HTTP {})", endpoint.getUrl(), response.getStatusCode().value());
                delivery.setStatus(DeliveryStatus.SUCCESS);
                delivery.setLastError(null);
                delivery.setNextRetryAt(null);
            } else {
                handleFailure(delivery, "HTTP " + response.getStatusCode().value() + " " + response.getBody());
            }

        } catch (HttpStatusCodeException ex) {
            handleFailure(delivery, "HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            handleFailure(delivery, "Connection/Timeout error: " + ex.getMessage());
        } catch (Exception ex) {
            handleFailure(delivery, "Unexpected error: " + ex.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    /**
     * Handles failure response and calculates next retry timestamp according to exponential backoff schedule.
     * Backoff schedule:
     * - Attempt 1 fail -> delay 10s (attempt 2)
     * - Attempt 2 fail -> delay 30s (attempt 3)
     * - Attempt 3 & 4 fail -> delay 120s (attempts 4 & 5)
     * - Attempt >= 5 fail -> status FAILED (routed to Dead-Letter)
     */
    private void handleFailure(WebhookDelivery delivery, String errorReason) {
        log.warn("Webhook delivery failed for tracking {} (Attempt {}/{}): {}",
                delivery.getTrackingNumber(), delivery.getAttempts(), MAX_ATTEMPTS, errorReason);

        delivery.setLastError(errorReason != null && errorReason.length() > 1000 ? errorReason.substring(0, 1000) : errorReason);

        if (delivery.getAttempts() >= MAX_ATTEMPTS) {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setNextRetryAt(null);
            log.error("Webhook delivery ID {} permanently FAILED after {} attempts. Flagged for DLQ / Manual Resend.",
                    delivery.getId(), delivery.getAttempts());
        } else {
            delivery.setStatus(DeliveryStatus.RETRYING);
            int delaySeconds = calculateBackoffDelaySeconds(delivery.getAttempts());
            delivery.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
            log.info("Scheduled retry for delivery ID {} in {}s (nextRetryAt: {})",
                    delivery.getId(), delaySeconds, delivery.getNextRetryAt());
        }
    }

    private int calculateBackoffDelaySeconds(int attemptsDone) {
        return switch (attemptsDone) {
            case 1 -> 10;
            case 2 -> 30;
            default -> 120; // 3 or 4
        };
    }

    /**
     * Manually triggers a resend of a failed or retrying delivery record.
     */
    @Transactional
    public WebhookDeliveryDto resendDelivery(Long deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("Webhook delivery not found with ID: " + deliveryId));

        WebhookEndpoint endpoint = delivery.getEndpoint();
        if (endpoint == null || !Boolean.TRUE.equals(endpoint.getActive())) {
            throw new IllegalStateException("Cannot resend webhook: endpoint is inactive or missing");
        }

        log.info("Manual resend initiated for webhook delivery ID {}", deliveryId);
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(delivery.getAttempts() + 1);
        executeDeliveryAttempt(delivery, endpoint, delivery.getPayload());

        return toDto(delivery);
    }

    /**
     * Sends a diagnostic test ping to the merchant's configured endpoint.
     */
    public TestPingResponse sendTestPing(Long merchantId) {
        WebhookEndpoint endpoint = endpointRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("No webhook endpoint configured for merchantId: " + merchantId));

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> pingPayload = Map.of(
                    "eventId", "evt_ping_" + UUID.randomUUID().toString().substring(0, 8),
                    "type", "TEST_PING",
                    "merchantId", merchantId,
                    "timestamp", Instant.now().toString(),
                    "message", "Test diagnostic ping from SwiftDrop webhook engine"
            );

            String rawJson = objectMapper.writeValueAsString(pingPayload);
            String signature = webhookSigner.sign(rawJson, endpoint.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(SIGNATURE_HEADER, signature);
            headers.set(HttpHeaders.USER_AGENT, "SwiftDrop-Webhook-Ping/1.0");

            HttpEntity<String> request = new HttpEntity<>(rawJson, headers);
            ResponseEntity<String> response = webhookRestTemplate.postForEntity(endpoint.getUrl(), request, String.class);
            long duration = System.currentTimeMillis() - start;

            boolean success = response.getStatusCode().is2xxSuccessful();
            return TestPingResponse.builder()
                    .success(success)
                    .httpStatus(response.getStatusCode().value())
                    .message(success ? "Ping succeeded: Server responded HTTP " + response.getStatusCode().value() : "Server returned non-2xx: " + response.getStatusCode().value())
                    .durationMs(duration)
                    .build();

        } catch (HttpStatusCodeException ex) {
            return TestPingResponse.builder()
                    .success(false)
                    .httpStatus(ex.getStatusCode().value())
                    .message("Server error: " + ex.getStatusCode().value())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception ex) {
            return TestPingResponse.builder()
                    .success(false)
                    .httpStatus(500)
                    .message("Failed to reach endpoint: " + ex.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    public WebhookDeliveryDto toDto(WebhookDelivery delivery) {
        return WebhookDeliveryDto.builder()
                .id(delivery.getId())
                .endpointId(delivery.getEndpoint() != null ? delivery.getEndpoint().getId() : null)
                .eventId(delivery.getEventId())
                .trackingNumber(delivery.getTrackingNumber())
                .payload(delivery.getPayload())
                .status(delivery.getStatus())
                .attempts(delivery.getAttempts())
                .lastError(delivery.getLastError())
                .lastAttemptAt(delivery.getLastAttemptAt())
                .nextRetryAt(delivery.getNextRetryAt())
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    public WebhookEndpointDto toEndpointDto(WebhookEndpoint endpoint) {
        return WebhookEndpointDto.builder()
                .id(endpoint.getId())
                .merchantId(endpoint.getMerchantId())
                .url(endpoint.getUrl())
                .secret(endpoint.getSecret())
                .active(endpoint.getActive())
                .createdAt(endpoint.getCreatedAt())
                .build();
    }
}
