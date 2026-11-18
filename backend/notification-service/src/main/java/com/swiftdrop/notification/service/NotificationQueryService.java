package com.swiftdrop.notification.service;

import com.swiftdrop.notification.common.NotFoundException;
import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.dto.MarkedReadResponse;
import com.swiftdrop.notification.dto.NotificationLogResponse;
import com.swiftdrop.notification.dto.NotificationResponse;
import com.swiftdrop.notification.dto.PageResponse;
import com.swiftdrop.notification.dto.UnreadCountResponse;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * Reads the notification log for the API. Writing rows stays in {@link NotificationService},
 * which the RabbitMQ consumer drives.
 */
@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private final NotificationLogRepository logs;

    /** The signed-in user's own notifications, newest first, whatever their role. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> feed(Long userId, int page, int size, boolean unreadOnly) {
        // The feed queries carry their own ORDER BY, so the Pageable must stay unsorted.
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> rows = unreadOnly
                ? logs.findUnreadRecipientFeed(userId, pageable)
                : logs.findRecipientFeed(userId, pageable);
        return PageResponse.of(rows, NotificationQueryService::toFeedItem);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(logs.countUnreadEvents(userId));
    }

    @Transactional
    public MarkedReadResponse markAllRead(Long userId) {
        return new MarkedReadResponse(logs.markAllRead(userId, Instant.now()));
    }

    /**
     * Marks one status change as read.
     *
     * @throws NotFoundException if the id does not exist or belongs to another user, so the
     *         response cannot be used to find out whether someone else's notification exists.
     */
    @Transactional
    public MarkedReadResponse markRead(Long id, Long userId) {
        NotificationLog row = logs.findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("No notification " + id + " for this account."));
        return new MarkedReadResponse(logs.markEventRead(row.getEventId(), userId, Instant.now()));
    }

    /** The admin and dispatcher log: one row per channel, with optional filters. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationLogResponse> adminLog(NotificationType type,
                                                          NotificationStatus status,
                                                          String trackingNumber,
                                                          Long customerId,
                                                          int page,
                                                          int size) {
        NotificationLog probe = NotificationLog.builder()
                .type(type)
                .status(status)
                .trackingNumber(normalise(trackingNumber))
                .customerId(customerId)
                .build();

        // Example.of ignores the null fields, so each filter is optional.
        Page<NotificationLog> rows = logs.findAll(
                Example.of(probe),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));

        return PageResponse.of(rows, NotificationQueryService::toAdminView);
    }

    private static String normalise(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            return null;
        }
        return trackingNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static NotificationResponse toFeedItem(NotificationLog row) {
        return new NotificationResponse(
                row.getId(),
                row.getParcelId(),
                row.getTrackingNumber(),
                row.getOldStatus(),
                row.getNewStatus(),
                message(row),
                row.getCreatedAt(),
                row.getReadAt() != null,
                row.getReadAt());
    }

    private static NotificationLogResponse toAdminView(NotificationLog row) {
        return new NotificationLogResponse(
                row.getId(),
                row.getEventId(),
                row.getParcelId(),
                row.getTrackingNumber(),
                row.getOldStatus(),
                row.getNewStatus(),
                row.getCustomerId(),
                row.getRecipientUserId(),
                row.getRecipientRole(),
                row.getType(),
                row.getRecipient(),
                row.getStatus(),
                row.getFailureReason(),
                row.getCreatedAt(),
                row.getSentAt());
    }

    /**
     * The same status change says something different to each role: the customer is being kept
     * informed, the courier is being told about work in their own hands, and staff are being told
     * something needs doing.
     */
    private static String message(NotificationLog row) {
        String tracking = row.getTrackingNumber();
        String status = readable(row.getNewStatus());
        String role = row.getRecipientRole() == null
                ? NotificationRecipient.CUSTOMER
                : row.getRecipientRole().toUpperCase(Locale.ROOT);

        return switch (role) {
            case NotificationRecipient.MERCHANT -> "Shipment " + tracking + " is now " + status + ".";
            case NotificationRecipient.COURIER ->
                    "Parcel " + tracking + " assigned to you is now " + status + ".";
            case NotificationRecipient.DISPATCHER, NotificationRecipient.ADMIN -> staffMessage(row, tracking);
            default -> "Parcel " + tracking + " is now " + status + ".";
        };
    }

    /** Staff only hear about the transitions that need a decision, so each one names the decision. */
    private static String staffMessage(NotificationLog row, String tracking) {
        String status = row.getNewStatus() == null ? "" : row.getNewStatus().toUpperCase(Locale.ROOT);
        return switch (status) {
            case "CREATED" -> "Parcel " + tracking + " was booked and is waiting to be assigned.";
            case "FAILED" -> "Delivery of parcel " + tracking + " failed and needs re-dispatch.";
            case "CANCELLED" -> "Parcel " + tracking + " was cancelled.";
            default -> "Parcel " + tracking + " is now " + readable(row.getNewStatus()) + ".";
        };
    }

    /** {@code OUT_FOR_DELIVERY} reads as {@code out for delivery} in the message text. */
    private static String readable(String status) {
        if (status == null) {
            return "updated";
        }
        return status.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
