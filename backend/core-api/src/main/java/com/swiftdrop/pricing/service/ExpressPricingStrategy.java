package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("EXPRESS")
public class ExpressPricingStrategy implements PricingStrategy {

    private final StandardPricingStrategy standardStrategy = new StandardPricingStrategy();

    @Override
    public BigDecimal calculateFee(PricingRule rule, ParcelSize size, BigDecimal weightKg, boolean isInterZone) {
        // Calculate standard fee first
        BigDecimal standardFee = standardStrategy.calculateFee(rule, size, weightKg, isInterZone);
        
        // Add 50% express surcharge
        BigDecimal expressMultiplier = new BigDecimal("1.50");
        return standardFee.multiply(expressMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
