package com.swiftdrop.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

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

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
