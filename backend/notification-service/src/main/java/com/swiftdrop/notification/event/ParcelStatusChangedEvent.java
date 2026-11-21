package com.swiftdrop.notification.event;

import java.time.Instant;
import java.util.UUID;

// Must stay field-for-field identical to core-api's com.swiftdrop.common.event.ParcelStatusChangedEvent (shared contract).
public record ParcelStatusChangedEvent(
    UUID eventId,
    Long parcelId,
    String trackingNumber,
    String oldStatus,
    String newStatus,
    Instant changedAt,
    String changedByRole,
    Long customerId,
    Long merchantId,
    String note,
    /** The FailureReason name on a FAILED transition, null on every other status. */
    String failureReason
) {

    /**
     * Keeps the ten-argument callers compiling, and lets a message published before this field
     * existed deserialize with no reason rather than failing.
     */
    public ParcelStatusChangedEvent(UUID eventId, Long parcelId, String trackingNumber, String oldStatus,
                                    String newStatus, Instant changedAt, String changedByRole,
                                    Long customerId, Long merchantId, String note) {
        this(eventId, parcelId, trackingNumber, oldStatus, newStatus, changedAt, changedByRole,
                customerId, merchantId, note, null);
    }
}
