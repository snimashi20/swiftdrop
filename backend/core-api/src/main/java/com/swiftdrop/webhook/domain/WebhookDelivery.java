package com.swiftdrop.webhook.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "webhook_deliveries",
    indexes = {
        @Index(name = "idx_webhook_deliveries_endpoint_id", columnList = "endpoint_id"),
        @Index(name = "idx_webhook_deliveries_status", columnList = "status"),
        @Index(name = "idx_webhook_deliveries_event_id", columnList = "event_id"),
        @Index(name = "idx_webhook_deliveries_tracking_number", columnList = "tracking_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private WebhookEndpoint endpoint;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "tracking_number", nullable = false, length = 16)
    private String trackingNumber;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
    }
}
