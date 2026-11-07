package com.swiftdrop.webhook.api;

import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.UpdateWebhookEndpointRequest;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
import com.swiftdrop.webhook.service.WebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Retrieves the webhook configuration for the current merchant.
     */
    @GetMapping("/endpoint")
    public ResponseEntity<WebhookEndpointDto> getEndpoint() {
        Long merchantId = webhookService.resolveCurrentMerchantId();
        return ResponseEntity.ok(webhookService.getEndpoint(merchantId));
    }

    /**
     * Registers or updates the webhook endpoint configuration for the current merchant.
     */
    @PutMapping("/endpoint")
    public ResponseEntity<WebhookEndpointDto> updateEndpoint(
            @Valid @RequestBody UpdateWebhookEndpointRequest request) {
        Long merchantId = webhookService.resolveCurrentMerchantId();
        return ResponseEntity.ok(webhookService.updateOrCreateEndpoint(merchantId, request));
    }

    /**
     * Regenerates a new HMAC signing secret for the current merchant's endpoint.
     */
    @PostMapping("/endpoint/secret")
    public ResponseEntity<Map<String, String>> regenerateSecret() {
        Long merchantId = webhookService.resolveCurrentMerchantId();
        String newSecret = webhookService.regenerateSecret(merchantId);
        return ResponseEntity.ok(Map.of("secret", newSecret));
    }

    /**
     * Sends an immediate test diagnostic ping to the configured webhook URL.
     */
    @PostMapping("/test-ping")
    public ResponseEntity<TestPingResponse> sendTestPing() {
        Long merchantId = webhookService.resolveCurrentMerchantId();
        return ResponseEntity.ok(webhookService.sendTestPing(merchantId));
    }

    /**
     * Retrieves the paginated delivery audit logs for the current merchant's endpoint.
     */
    @GetMapping("/deliveries")
    public ResponseEntity<Page<WebhookDeliveryDto>> getDeliveries(
            @PageableDefault(size = 10) Pageable pageable) {
        Long merchantId = webhookService.resolveCurrentMerchantId();
        return ResponseEntity.ok(webhookService.getDeliveries(merchantId, pageable));
    }

    /**
     * Manually triggers an immediate retry for a specific delivery ID.
     */
    @PostMapping("/deliveries/{deliveryId}/resend")
    public ResponseEntity<WebhookDeliveryDto> resendDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(webhookService.resendDelivery(deliveryId));
    }
}
