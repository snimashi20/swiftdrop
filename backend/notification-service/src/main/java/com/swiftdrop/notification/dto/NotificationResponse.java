package com.swiftdrop.notification.dto;

import java.time.Instant;

/**
 * One status change as the customer sees it: a single item even when it was both e-mailed and
 * texted. Nothing about the channel or the recipient is exposed here.
 */
public record NotificationResponse(
        Long id,
        Long parcelId,
        String trackingNumber,
        String oldStatus,
        String newStatus,
        String message,
        Instant createdAt,
        boolean read,
        Instant readAt
) {
}
