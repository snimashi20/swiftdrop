package com.swiftdrop.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// Table is created by core-api's Flyway migrations (V202610262300, V202610311800, V202611041000,
// V202611141100); keep this mapping in sync.
@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false)
    private Long parcelId;

    @Column(nullable = false, length = 12)
    private String trackingNumber;

    @Column(length = 30)
    private String oldStatus;

    @Column(nullable = false, length = 30)
    private String newStatus;

    @Column(nullable = false)
    private Long customerId;

    /** The user this row was written for: the customer, the merchant, the courier or a staff user. */
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    /**
     * The recipient's role when the row was written. Denormalised on purpose: it decides how the
     * feed item is worded, and a later role change must not rewrite history.
     */
    @Column(name = "recipient_role", nullable = false, length = 20)
    private String recipientRole;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    /** When the recipient saw this notification; null means unread. */
    private Instant readAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
