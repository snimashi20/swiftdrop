package com.swiftdrop.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {
    private String eventId;
    private Long parcelId;
    private String trackingNumber;
    private String oldStatus;
    private String newStatus;
    private Instant changedAt;
    private String changedByRole;
    private String note;
}
