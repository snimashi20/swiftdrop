package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculateFee(PricingRule rule, ParcelSize size, double weightKg, boolean isInterZone) {
        BigDecimal base = rule.getBaseFee();
        BigDecimal weight = BigDecimal.valueOf(weightKg);
        
        // Base fee + (weight * per kg fee)
        BigDecimal total = base.add(weight.multiply(rule.getPerKgFee()));

        // Apply size factor
        BigDecimal sizeFactor = switch (size) {
            case S -> rule.getSizeFactorS();
            case M -> rule.getSizeFactorM();
            case L -> rule.getSizeFactorL();
        };
        total = total.multiply(sizeFactor);

        // Apply inter-zone surcharge if applicable
        if (isInterZone) {
            total = total.add(rule.getInterZoneSurcharge());
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
