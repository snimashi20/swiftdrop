package com.swiftdrop.pricing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PricingRuleRequest {
    @NotNull(message = "Zone ID is required")
    private Long zoneId;

    @NotNull(message = "Base fee is required")
    @Positive(message = "Base fee must be positive")
    private BigDecimal baseFee;

    @NotNull(message = "Per kg fee is required")
    @Positive(message = "Per kg fee must be positive")
    private BigDecimal perKgFee;

    @NotNull(message = "Inter-zone surcharge is required")
    @Positive(message = "Inter-zone surcharge must be positive")
    private BigDecimal interZoneSurcharge;

    // Optional: if null, defaults to 1.00, 1.50, 2.00
    private BigDecimal sizeFactorS;
    private BigDecimal sizeFactorM;
    private BigDecimal sizeFactorL;
}
