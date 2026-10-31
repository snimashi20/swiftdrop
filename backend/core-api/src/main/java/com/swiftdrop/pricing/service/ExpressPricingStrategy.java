package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component("EXPRESS")
@RequiredArgsConstructor
public class ExpressPricingStrategy implements PricingStrategy {

    private final StandardPricingStrategy standardStrategy;

    @Override
    public BigDecimal calculateFee(PricingRule rule, ParcelSize size, BigDecimal weightKg, boolean isInterZone) {
        BigDecimal standardFee = standardStrategy.calculateFee(rule, size, weightKg, isInterZone);
        BigDecimal expressMultiplier = new BigDecimal("1.50");
        return standardFee.multiply(expressMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
