package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateFee(PricingRule rule, ParcelSize size, BigDecimal weightKg, boolean isInterZone) {
        
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero");
        }

        BigDecimal base = rule.getBaseFee();
        BigDecimal total = base.add(weightKg.multiply(rule.getPerKgFee()));

        BigDecimal sizeFactor = switch (size) {
            case S -> rule.getSizeFactorS();
            case M -> rule.getSizeFactorM();
            case L -> rule.getSizeFactorL();
        };
        total = total.multiply(sizeFactor);

        if (isInterZone) {
            total = total.add(rule.getInterZoneSurcharge());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
