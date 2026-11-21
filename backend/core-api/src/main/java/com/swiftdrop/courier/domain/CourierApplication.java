package com.swiftdrop.courier.domain;

import com.swiftdrop.dispatch.domain.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A rider's request to join the delivery fleet. An admin reviews it before the
 * linked COURIER user account is activated. Mirrors the merchant approval flow.
 */
@Entity
@Table(
    name = "courier_applications",
    indexes = {
        @Index(name = "idx_courier_applications_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 30)
    private VehicleType vehicleType;

    @Column(name = "preferred_zone", length = 100)
    private String preferredZone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CourierApplicationStatus status = CourierApplicationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = CourierApplicationStatus.PENDING;
        }
    }
}
