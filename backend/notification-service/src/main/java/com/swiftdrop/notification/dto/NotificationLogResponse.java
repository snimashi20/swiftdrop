package com.swiftdrop.notification.dto;

import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

/**
 * One delivery attempt for the admin log: a row per channel.
 *
 * <p>The recipient is the masked value the consumer stored, so no full e-mail address or phone
 * number ever leaves the service. {@code recipientUserId} and {@code recipientRole} say who the row
 * was written for, which is no longer always the customer.
 */
public record NotificationLogResponse(
        Long id,
        UUID eventId,
        Long parcelId,
        String trackingNumber,
        String oldStatus,
        String newStatus,
        Long customerId,
        Long recipientUserId,
        String recipientRole,
        NotificationType type,
        String recipient,
        NotificationStatus status,
        String failureReason,
        Instant createdAt,
        Instant sentAt
) {
}
