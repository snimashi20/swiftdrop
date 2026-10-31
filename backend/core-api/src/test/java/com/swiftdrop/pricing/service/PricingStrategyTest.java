package com.swiftdrop.pricing.service;

import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.domain.PricingRule;
import com.swiftdrop.pricing.domain.Zone;
import com.swiftdrop.pricing.dto.QuoteRequest;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class PricingServiceTest {

    private PricingService pricingService;
    private ZoneRepository zoneRepository;
    private PricingRuleRepository pricingRuleRepository;

    @BeforeEach
    void setUp() {
        zoneRepository = Mockito.mock(ZoneRepository.class);
        pricingRuleRepository = Mockito.mock(PricingRuleRepository.class);

        Map<String, PricingStrategy> strategies = Map.of(
                "STANDARD", new StandardPricingStrategy(),
                "EXPRESS", new ExpressPricingStrategy(new StandardPricingStrategy())
        );

        pricingService = new PricingService(zoneRepository, pricingRuleRepository, strategies);
        
        ReflectionTestUtils.setField(pricingService, "activeStrategyName", "STANDARD");
    }

    @Test
    void calculateQuote_Success() {
        Zone pickup = Zone.builder().id(1L).name("Colombo").build();
        Zone dropoff = Zone.builder().id(2L).name("Kandy").build();
        PricingRule rule = PricingRule.builder()
                .baseFee(new BigDecimal("300.00"))
                .perKgFee(new BigDecimal("100.00"))
                .interZoneSurcharge(new BigDecimal("250.00"))
                .sizeFactorM(new BigDecimal("1.50"))
                .build();

        when(zoneRepository.findByDistrict("Colombo")).thenReturn(Optional.of(pickup));
        when(zoneRepository.findByDistrict("Kandy")).thenReturn(Optional.of(dropoff));
        when(pricingRuleRepository.findByZoneId(1L)).thenReturn(Optional.of(rule));

        QuoteRequest request = new QuoteRequest();
        request.setPickupDistrict("Colombo");
        request.setDropoffDistrict("Kandy");
        request.setSize(ParcelSize.M);
        request.setWeightKg(new BigDecimal("2.0"));

        QuoteResponse response = pricingService.calculateQuote(request);

        assertEquals(new BigDecimal("1000.00"), response.getFee());
        assertEquals(1L, response.getPickupZoneId());
        assertEquals(2L, response.getDropoffZoneId());
    }
}
