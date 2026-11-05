package com.swiftdrop.notification.api;

import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.dto.MarkedReadResponse;
import com.swiftdrop.notification.dto.NotificationLogResponse;
import com.swiftdrop.notification.dto.NotificationResponse;
import com.swiftdrop.notification.dto.PageResponse;
import com.swiftdrop.notification.dto.UnreadCountResponse;
import com.swiftdrop.notification.security.AuthenticatedUser;
import com.swiftdrop.notification.service.NotificationQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The notifications API of the notification service.
 *
 * <p>Every endpoint needs the Bearer token core-api issues. A customer only ever sees their own
 * rows, because the customer id comes from the token and never from a parameter.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationQueryService notifications;

    /** The signed-in user's notifications, one item per status change, newest first. */
    @GetMapping("/me")
    public PageResponse<NotificationResponse> myNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notifications.feed(user.id(), page, size, unreadOnly);
    }

    /** The number for the notification bell. */
    @GetMapping("/me/unread-count")
    public UnreadCountResponse myUnreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return notifications.unreadCount(user.id());
    }

    /** Marks every unread notification of the signed-in user as read. */
    @PostMapping("/me/read-all")
    public MarkedReadResponse markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return notifications.markAllRead(user.id());
    }

    /** Marks one notification of the signed-in user as read; 404 if it belongs to someone else. */
    @PostMapping("/{id}/read")
    public MarkedReadResponse markRead(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id) {
        return notifications.markRead(id, user.id());
    }

    /**
     * The delivery log for the admin notification log page: one row per channel, recipients masked.
     * All filters are optional.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public PageResponse<NotificationLogResponse> adminLog(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String trackingNumber,
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return notifications.adminLog(type, status, trackingNumber, customerId, page, size);
    }
}
