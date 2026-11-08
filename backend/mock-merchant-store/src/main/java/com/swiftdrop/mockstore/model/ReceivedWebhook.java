package com.swiftdrop.mockstore.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceivedWebhook {
    private String id;
    private Instant receivedAt;
    private String trackingNumber;
    private String eventId;
    private String oldStatus;
    private String newStatus;
    private String signature;
    private boolean signatureValid;
    private String rawPayload;
    private int httpStatusReturned;
}
