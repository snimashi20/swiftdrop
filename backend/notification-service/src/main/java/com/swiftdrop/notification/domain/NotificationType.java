package com.swiftdrop.notification.domain;

/**
 * The channel a notification row was delivered on.
 *
 * <p>{@code IN_APP} has no gateway behind it: the row in {@code notification_log} <em>is</em> the
 * notification, and the feed endpoints are what deliver it. Only a customer gets {@code EMAIL} or
 * {@code SMS}; staff-facing roles are in-app only.
 */
public enum NotificationType {
    IN_APP,
    EMAIL,
    SMS
}
