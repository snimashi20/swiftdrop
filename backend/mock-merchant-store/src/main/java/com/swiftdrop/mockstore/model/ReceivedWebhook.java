package com.swiftdrop.mockstore.model;

import java.time.Instant;

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

    private ReceivedWebhook(Builder builder) {
        this.id = builder.id;
        this.receivedAt = builder.receivedAt;
        this.trackingNumber = builder.trackingNumber;
        this.eventId = builder.eventId;
        this.oldStatus = builder.oldStatus;
        this.newStatus = builder.newStatus;
        this.signature = builder.signature;
        this.signatureValid = builder.signatureValid;
        this.rawPayload = builder.rawPayload;
        this.httpStatusReturned = builder.httpStatusReturned;
    }

    public String getId() { return id; }
    public Instant getReceivedAt() { return receivedAt; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getEventId() { return eventId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getSignature() { return signature; }
    public boolean isSignatureValid() { return signatureValid; }
    public String getRawPayload() { return rawPayload; }
    public int getHttpStatusReturned() { return httpStatusReturned; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
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

        public Builder id(String id) { this.id = id; return this; }
        public Builder receivedAt(Instant receivedAt) { this.receivedAt = receivedAt; return this; }
        public Builder trackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; return this; }
        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder oldStatus(String oldStatus) { this.oldStatus = oldStatus; return this; }
        public Builder newStatus(String newStatus) { this.newStatus = newStatus; return this; }
        public Builder signature(String signature) { this.signature = signature; return this; }
        public Builder signatureValid(boolean signatureValid) { this.signatureValid = signatureValid; return this; }
        public Builder rawPayload(String rawPayload) { this.rawPayload = rawPayload; return this; }
        public Builder httpStatusReturned(int httpStatusReturned) { this.httpStatusReturned = httpStatusReturned; return this; }
        public ReceivedWebhook build() { return new ReceivedWebhook(this); }
    }
}
