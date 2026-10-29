package com.swiftdrop.pricing.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", unique = true)
    private Zone zone;

    @Column(name = "base_fee", nullable = false)
    private BigDecimal baseFee;

    @Column(name = "per_kg_fee", nullable = false)
    private BigDecimal perKgFee;

    @Column(name = "inter_zone_surcharge", nullable = false)
    private BigDecimal interZoneSurcharge;

    @Builder.Default
    @Column(name = "size_factor_s", nullable = false)
    private BigDecimal sizeFactorS = new BigDecimal("1.00");

    @Builder.Default
    @Column(name = "size_factor_m", nullable = false)
    private BigDecimal sizeFactorM = new BigDecimal("1.50");

    @Builder.Default
    @Column(name = "size_factor_l", nullable = false)
    private BigDecimal sizeFactorL = new BigDecimal("2.00");
}
