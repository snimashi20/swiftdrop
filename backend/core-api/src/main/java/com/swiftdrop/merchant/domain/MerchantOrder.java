package com.swiftdrop.merchant.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "merchant_orders",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_merchant_orders_merchant_idempotency",
            columnNames = {"merchant_id", "idempotency_key"}
        )
    },
    indexes = {
        @Index(name = "idx_merchant_orders_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_merchant_orders_parcel_id", columnList = "parcel_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "parcel_id")
    private Long parcelId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "external_order_id", length = 100)
    private String externalOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
