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
    String note
) {}
