package com.swiftdrop.notification.domain;

// Must stay in step with notification-service's com.swiftdrop.notification.domain.NotificationType:
// both map the type column of notification_log, and a value missing here fails on read.
public enum NotificationType {
    IN_APP,
    EMAIL,
    SMS
}
