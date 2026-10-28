package com.swiftdrop.mockstore.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ReceivedWebhook {
    private final String id;
    private final Instant receivedAt;
    private final String trackingNumber;
    private final String eventId;
    private final String oldStatus;
    private final String newStatus;
    private final String signature;
    private final boolean signatureValid;
    private final String rawPayload;
    private final int httpStatusReturned;
}
