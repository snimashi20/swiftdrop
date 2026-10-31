package com.swiftdrop.mockstore.controller;

import com.swiftdrop.mockstore.model.ReceivedWebhook;
import com.swiftdrop.mockstore.service.WebhookReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WebhookReceiverController {

    private static final Logger log = LoggerFactory.getLogger(WebhookReceiverController.class);

    private final WebhookReceiverService receiverService;
    private final String storeName;

    public WebhookReceiverController(
            WebhookReceiverService receiverService,
            @Value("${mock-merchant.store-name:Apex Electronics (Mock Merchant Store)}") String storeName) {
        this.receiverService = receiverService;
        this.storeName = storeName;
    }

    /**
     * Endpoint where SwiftDrop core-api webhook engine POSTs events.
     * Header: X-SwiftDrop-Signature contains the HMAC-SHA256 hex digest.
     */
    @PostMapping(value = "/api/webhooks/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> receiveWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-SwiftDrop-Signature", required = false) String signatureHeader) {

        int simStatus = receiverService.getSimulationStatusCode();
        if (simStatus >= 400) {
            log.warn("Mock store simulated failure mode active (status {}). Rejecting webhook.", simStatus);
            // Record failure attempt so the UI table shows the red status
            receiverService.recordDelivery(rawPayload, signatureHeader != null ? signatureHeader : "MISSING");
            return ResponseEntity.status(simStatus)
                    .body(Map.of(
                            "error", "Simulated Store Server Error",
                            "status", simStatus,
                            "message", "Mock store server is currently simulating an outage"
                    ));
        }

        log.info("Received incoming webhook delivery. Signature header present: {}", signatureHeader != null);
        ReceivedWebhook recorded = receiverService.recordDelivery(rawPayload, signatureHeader);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "id", recorded.getId(),
                "signatureValid", recorded.isSignatureValid(),
                "receivedAt", recorded.getReceivedAt().toString()
        ));
    }

    /**
     * Returns all received deliveries in JSON format.
     */
    @GetMapping(value = "/api/webhooks/orders", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReceivedWebhook> listWebhooksJson() {
        return receiverService.getAllDeliveries();
    }

    /**
     * Simulation toggle: sets the store to 200 (OK) or 500 (Internal Server Error)
     * Used during the live demo to show retries in action.
     */
    @PostMapping("/api/sim/status")
    public ResponseEntity<?> setSimulatedStatus(@RequestParam(defaultValue = "200") int code) {
        receiverService.setSimulationStatusCode(code);
        log.info("Simulated store status updated to: {}", code);
        return ResponseEntity.ok(Map.of(
                "message", "Store status code updated",
                "simulatedStatusCode", code
        ));
    }

    /**
     * Clears all received webhook records.
     */
    @PostMapping("/api/webhooks/clear")
    public ResponseEntity<?> clearDeliveries() {
        receiverService.clear();
        return ResponseEntity.ok(Map.of("message", "Delivery logs cleared"));
    }

    /**
     * Clean, modern HTML dashboard rendered directly at root (http://localhost:8085).
     * Auto-refreshes every 3 seconds to show live webhook deliveries during demos.
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String dashboardHtml() {
        List<ReceivedWebhook> deliveries = receiverService.getAllDeliveries();
        int simCode = receiverService.getSimulationStatusCode();

        StringBuilder rows = new StringBuilder();
        if (deliveries.isEmpty()) {
            rows.append("<tr><td colspan='7' style='padding: 24px; text-align: center; color: #6B7280; font-style: italic;'>")
                .append("No webhook deliveries received yet. Trigger a parcel status change in SwiftDrop to see it appear here live!")
                .append("</td></tr>");
        } else {
            for (ReceivedWebhook d : deliveries) {
                boolean valid = d.isSignatureValid();
                boolean isSuccess = d.getHttpStatusReturned() == 200;

                String statusBadge = isSuccess
                        ? "<span style='background:#DCFCE7; color:#15803D; padding:3px 8px; border-radius:999px; font-weight:600; font-size:11px;'>200 OK</span>"
                        : "<span style='background:#FEE2E2; color:#B91C1C; padding:3px 8px; border-radius:999px; font-weight:600; font-size:11px;'>" + d.getHttpStatusReturned() + " ERROR</span>";

                String sigBadge = valid
                        ? "<span style='background:#EFF6FF; color:#1E3A5F; padding:3px 8px; border-radius:999px; font-weight:600; font-size:11px;'>VERIFIED (HMAC-SHA256)</span>"
                        : "<span style='background:#FEF9C3; color:#854D0E; padding:3px 8px; border-radius:999px; font-weight:600; font-size:11px;'>INVALID / MISSING</span>";

                rows.append("<tr style='border-bottom: 1px solid #E5E7EB; font-size: 13px;'>")
                    .append("<td style='padding: 12px 14px; color: #1E3A5F; font-weight: 600;'>").append(d.getTrackingNumber()).append("</td>")
                    .append("<td style='padding: 12px 14px; color: #4B5563;'>").append(d.getOldStatus()).append(" &rarr; <b>").append(d.getNewStatus()).append("</b></td>")
                    .append("<td style='padding: 12px 14px;'>").append(statusBadge).append("</td>")
                    .append("<td style='padding: 12px 14px;'>").append(sigBadge).append("</td>")
                    .append("<td style='padding: 12px 14px; font-family: monospace; font-size: 11px; color: #4B5563;'>").append(d.getSignature() != null && d.getSignature().length() > 16 ? d.getSignature().substring(0, 16) + "..." : d.getSignature()).append("</td>")
                    .append("<td style='padding: 12px 14px; color: #6B7280; font-size: 12px;'>").append(d.getReceivedAt()).append("</td>")
                    .append("<td style='padding: 12px 14px;'><button onclick=\"alert('").append(escapeJs(d.getRawPayload())).append("')\" style='background:#F3F4F6; border:1px solid #D1D5DB; padding:4px 8px; border-radius:4px; font-size:11px; cursor:pointer;'>View JSON</button></td>")
                    .append("</tr>");
            }
        }

        String simBanner = simCode == 200
                ? "<div style='display:inline-block; background:#DCFCE7; color:#166534; padding:6px 14px; border-radius:6px; font-weight:600; font-size:13px;'>Store Health: Normal (HTTP 200)</div>"
                : "<div style='display:inline-block; background:#FEE2E2; color:#991B1B; padding:6px 14px; border-radius:6px; font-weight:600; font-size:13px;'>Store Health: Simulating Outage (HTTP 500)</div>";

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='utf-8'><title>" + storeName + "</title>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<meta http-equiv='refresh' content='3'>"
                + "<style>body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; background:#F9FAFB; margin:0; padding:24px; color:#111827;}</style>"
                + "</head><body>"
                + "<div style='max-width:1200px; margin:0 auto; background:#FFFFFF; border:1px solid #E5E7EB; border-radius:12px; box-shadow:0 1px 3px rgba(0,0,0,0.05); padding:28px;'>"
                + "<div style='display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #E5E7EB; padding-bottom:18px; margin-bottom:20px;'>"
                + "<div>"
                + "<h1 style='font-size:22px; color:#1E3A5F; margin:0 0 6px 0;'>" + storeName + "</h1>"
                + "<div style='font-size:13px; color:#6B7280;'>Webhook Receiver Endpoint: <code style='background:#F3F4F6; padding:2px 6px; border-radius:4px; color:#1E3A5F;'>POST http://localhost:8085/api/webhooks/orders</code></div>"
                + "</div>"
                + "<div>" + simBanner + "</div>"
                + "</div>"
                + "<div style='display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;'>"
                + "<div style='font-size:14px; font-weight:600; color:#374151;'>Live Deliveries Feed (" + deliveries.size() + ") <span style='font-size:11px; font-weight:normal; color:#9CA3AF;'>· Auto-refreshes every 3s</span></div>"
                + "<div style='display:flex; gap:8px;'>"
                + "<button onclick=\"fetch('/api/sim/status?code=500', {method:'POST'}).then(()=>location.reload())\" style='background:#FEF2F2; color:#991B1B; border:1px solid #FCA5A5; padding:6px 12px; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer;'>Simulate Outage (500)</button>"
                + "<button onclick=\"fetch('/api/sim/status?code=200', {method:'POST'}).then(()=>location.reload())\" style='background:#F0FDF4; color:#166534; border:1px solid #86EFAC; padding:6px 12px; border-radius:6px; font-size:12px; font-weight:600; cursor:pointer;'>Restore Normal (200)</button>"
                + "<button onclick=\"fetch('/api/webhooks/clear', {method:'POST'}).then(()=>location.reload())\" style='background:#F3F4F6; color:#4B5563; border:1px solid #D1D5DB; padding:6px 12px; border-radius:6px; font-size:12px; cursor:pointer;'>Clear Feed</button>"
                + "</div>"
                + "</div>"
                + "<div style='overflow-x:auto;'>"
                + "<table style='width:100%; border-collapse:collapse; text-align:left;'>"
                + "<thead><tr style='background:#F9FAFB; border-bottom:1px solid #E5E7EB; font-size:11px; text-transform:uppercase; color:#6B7280;'>"
                + "<th style='padding:10px 14px;'>Tracking #</th>"
                + "<th style='padding:10px 14px;'>Status Transition</th>"
                + "<th style='padding:10px 14px;'>Response</th>"
                + "<th style='padding:10px 14px;'>HMAC Verification</th>"
                + "<th style='padding:10px 14px;'>Signature (Prefix)</th>"
                + "<th style='padding:10px 14px;'>Timestamp</th>"
                + "<th style='padding:10px 14px;'>Action</th>"
                + "</tr></thead>"
                + "<tbody>" + rows.toString() + "</tbody>"
                + "</table>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    private String escapeJs(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");
    }
}
