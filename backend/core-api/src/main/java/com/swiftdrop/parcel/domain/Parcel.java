package com.swiftdrop.parcel.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 12)
    private String trackingNumber;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParcelStatus status = ParcelStatus.CREATED;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "line1", column = @Column(name = "pickup_line1", nullable = false, length = 200)),
        @AttributeOverride(name = "city", column = @Column(name = "pickup_city", nullable = false, length = 100)),
        @AttributeOverride(name = "district", column = @Column(name = "pickup_district", nullable = false, length = 100))
    })
    private Address pickup;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "line1", column = @Column(name = "dropoff_line1", nullable = false, length = 200)),
        @AttributeOverride(name = "city", column = @Column(name = "dropoff_city", nullable = false, length = 100)),
        @AttributeOverride(name = "district", column = @Column(name = "dropoff_district", nullable = false, length = 100))
    })
    private Address dropoff;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 1)
    private ParcelSize size;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

        @Column(name = "assigned_courier_id")
    private Long assignedCourierId;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "delivery_instructions", length = 500)
    private String deliveryInstructions;

    @Column(name = "failure_reason", length = 50)
    private String failureReason;

    @Column(name = "failure_note", length = 500)
    private String failureNote;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}