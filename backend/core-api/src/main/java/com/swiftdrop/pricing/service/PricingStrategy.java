package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculateFee(PricingRule rule, ParcelSize size, double weightKg, boolean isInterZone);
}
