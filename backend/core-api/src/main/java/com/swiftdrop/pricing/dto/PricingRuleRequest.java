package com.swiftdrop.pricing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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
    @PositiveOrZero(message = "Inter-zone surcharge must be zero or positive")
    private BigDecimal interZoneSurcharge;

    @Positive(message = "Size factor S must be positive")
    private BigDecimal sizeFactorS;
    
    @Positive(message = "Size factor M must be positive")
    private BigDecimal sizeFactorM;
    
    @Positive(message = "Size factor L must be positive")
    private BigDecimal sizeFactorL;
}
