package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTest {

    private final StandardPricingStrategy standardStrategy = new StandardPricingStrategy();
    private final ExpressPricingStrategy expressStrategy = new ExpressPricingStrategy();

    @Test
    void testStandardPricing_SameZone() {
        PricingRule rule = getDummyRule();
        // Base (300) + (2kg * 100) = 500. Size M factor (1.5) = 750. No surcharge.
        BigDecimal fee = standardStrategy.calculateFee(rule, ParcelSize.M, new BigDecimal("2.0"), false);
        assertEquals(new BigDecimal("750.00"), fee);
    }

    @Test
    void testStandardPricing_InterZone() {
        PricingRule rule = getDummyRule();
        // Base (300) + (2kg * 100) = 500. Size M factor (1.5) = 750. + Surcharge (250) = 1000.
        BigDecimal fee = standardStrategy.calculateFee(rule, ParcelSize.M, new BigDecimal("2.0"), true);
        assertEquals(new BigDecimal("1000.00"), fee);
    }

    @Test
    void testExpressPricing() {
        PricingRule rule = getDummyRule();
        // Standard 750 * 1.5 = 1125.
        BigDecimal fee = expressStrategy.calculateFee(rule, ParcelSize.M, new BigDecimal("2.0"), false);
        assertEquals(new BigDecimal("1125.00"), fee);
    }

    @Test
    void testInvalidWeight() {
        PricingRule rule = getDummyRule();
        assertThrows(IllegalArgumentException.class, () -> 
            standardStrategy.calculateFee(rule, ParcelSize.S, BigDecimal.ZERO, false)
        );
    }

    private PricingRule getDummyRule() {
        return PricingRule.builder()
                .baseFee(new BigDecimal("300.00"))
                .perKgFee(new BigDecimal("100.00"))
                .interZoneSurcharge(new BigDecimal("250.00"))
                .sizeFactorS(new BigDecimal("1.00"))
                .sizeFactorM(new BigDecimal("1.50"))
                .sizeFactorL(new BigDecimal("2.00"))
                .build();
    }
}
