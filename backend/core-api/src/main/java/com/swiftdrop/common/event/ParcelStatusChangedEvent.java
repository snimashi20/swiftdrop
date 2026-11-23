package com.swiftdrop.common.event;

import java.time.Instant;
import java.util.UUID;

// Must stay field-for-field identical to notification-service's
// com.swiftdrop.notification.event.ParcelStatusChangedEvent (shared contract).
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
     * Keeps the ten-argument callers compiling. A transition that is not a failure carries no
     * reason, so this is the right shape for all of them.
     */
    public ParcelStatusChangedEvent(UUID eventId, Long parcelId, String trackingNumber, String oldStatus,
                                    String newStatus, Instant changedAt, String changedByRole,
                                    Long customerId, Long merchantId, String note) {
        this(eventId, parcelId, trackingNumber, oldStatus, newStatus, changedAt, changedByRole,
                customerId, merchantId, note, null);
    }
}
