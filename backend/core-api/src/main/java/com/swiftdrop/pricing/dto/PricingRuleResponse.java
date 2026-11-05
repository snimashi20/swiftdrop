package com.swiftdrop.pricing.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PricingRuleResponse {
    private Long id;
    private Long zoneId;
    private String zoneName;
    private BigDecimal baseFee;
    private BigDecimal perKgFee;
    private BigDecimal interZoneSurcharge;
    private BigDecimal sizeFactorS;
    private BigDecimal sizeFactorM;
    private BigDecimal sizeFactorL;
}
