package com.swiftdrop.common.event;

import java.time.Instant;
import java.util.UUID;

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
