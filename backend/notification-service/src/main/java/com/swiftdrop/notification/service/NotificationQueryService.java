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

    /** The signed-in customer's own status changes, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> feed(Long customerId, int page, int size, boolean unreadOnly) {
        // The feed queries carry their own ORDER BY, so the Pageable must stay unsorted.
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> rows = unreadOnly
                ? logs.findUnreadCustomerFeed(customerId, pageable)
                : logs.findCustomerFeed(customerId, pageable);
        return PageResponse.of(rows, NotificationQueryService::toCustomerView);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long customerId) {
        return new UnreadCountResponse(logs.countUnreadEvents(customerId));
    }

    @Transactional
    public MarkedReadResponse markAllRead(Long customerId) {
        return new MarkedReadResponse(logs.markAllRead(customerId, Instant.now()));
    }

    /**
     * Marks one status change as read.
     *
     * @throws NotFoundException if the id does not exist or belongs to another customer, so the
     *         response cannot be used to find out whether someone else's notification exists.
     */
    @Transactional
    public MarkedReadResponse markRead(Long id, Long customerId) {
        NotificationLog row = logs.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException("No notification " + id + " for this account."));
        return new MarkedReadResponse(logs.markEventRead(row.getEventId(), customerId, Instant.now()));
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

    private static NotificationResponse toCustomerView(NotificationLog row) {
        return new NotificationResponse(
                row.getId(),
                row.getParcelId(),
                row.getTrackingNumber(),
                row.getOldStatus(),
                row.getNewStatus(),
                "Parcel " + row.getTrackingNumber() + " is now " + readable(row.getNewStatus()) + ".",
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
                row.getType(),
                row.getRecipient(),
                row.getStatus(),
                row.getFailureReason(),
                row.getCreatedAt(),
                row.getSentAt());
    }

    /** {@code OUT_FOR_DELIVERY} reads as {@code out for delivery} in the message text. */
    private static String readable(String status) {
        if (status == null) {
            return "updated";
        }
        return status.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
