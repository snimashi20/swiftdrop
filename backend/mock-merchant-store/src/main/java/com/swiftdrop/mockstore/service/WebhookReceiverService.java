package com.swiftdrop.mockstore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.mockstore.model.ReceivedWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WebhookReceiverService {

    private static final Logger log = LoggerFactory.getLogger(WebhookReceiverService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String webhookSecret;
    private final ObjectMapper objectMapper;
    private final List<ReceivedWebhook> deliveries = new CopyOnWriteArrayList<>();
    private volatile int simulationStatusCode = 200;

    public WebhookReceiverService(
            @Value("${mock-merchant.webhook-secret:dev-webhook-secret-key-12345}") String webhookSecret,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.objectMapper = objectMapper;
    }

    public ReceivedWebhook recordDelivery(String payload, String signatureHeader) {
        boolean valid = verifyHmacSha256(payload, signatureHeader, webhookSecret);
        String trackingNumber = "UNKNOWN";
        String eventId = "UNKNOWN";
        String oldStatus = "UNKNOWN";
        String newStatus = "UNKNOWN";

        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("trackingNumber")) trackingNumber = node.get("trackingNumber").asText();
            if (node.has("eventId")) eventId = node.get("eventId").asText();
            if (node.has("oldStatus")) oldStatus = node.get("oldStatus").asText();
            if (node.has("newStatus")) newStatus = node.get("newStatus").asText();
        } catch (Exception e) {
            log.warn("Could not parse webhook JSON payload: {}", e.getMessage());
        }

        ReceivedWebhook webhook = ReceivedWebhook.builder()
                .id(UUID.randomUUID().toString())
                .receivedAt(Instant.now())
                .trackingNumber(trackingNumber)
                .eventId(eventId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .signature(signatureHeader)
                .signatureValid(valid)
                .rawPayload(payload)
                .httpStatusReturned(simulationStatusCode)
                .build();

        deliveries.add(0, webhook);
        return webhook;
    }

    public boolean verifyHmacSha256(String payload, String signatureHeader, String secret) {
        if (payload == null || signatureHeader == null || secret == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return MessageDigest.isEqual(
                    hexString.toString().getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Error computing HMAC-SHA256: {}", e.getMessage());
            return false;
        }
    }

    public List<ReceivedWebhook> getAllDeliveries() {
        return Collections.unmodifiableList(deliveries);
    }

    public void clear() {
        deliveries.clear();
    }

    public int getSimulationStatusCode() {
        return simulationStatusCode;
    }

    public void setSimulationStatusCode(int simulationStatusCode) {
        this.simulationStatusCode = simulationStatusCode;
    }
}
