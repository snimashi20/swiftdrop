package com.swiftdrop.webhook.dto;

import com.swiftdrop.webhook.domain.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryDto {
    private Long id;
    private Long endpointId;
    private String eventId;
    private String trackingNumber;
    private String payload;
    private DeliveryStatus status;
    private Integer attempts;
    private String lastError;
    private Instant lastAttemptAt;
    private Instant nextRetryAt;
    private Instant createdAt;
}
