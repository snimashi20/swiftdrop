package com.swiftdrop.pricing.service;

import com.swiftdrop.common.exception.NotFoundException; // Adjust package if M8 used a different one
import com.swiftdrop.pricing.domain.PricingRule;
import com.swiftdrop.pricing.domain.Zone;
import com.swiftdrop.pricing.dto.QuoteRequest;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final Map<String, PricingStrategy> strategies;

    @Value("${PRICING_STRATEGY:STANDARD}")
    private String activeStrategyName;

    public QuoteResponse calculateQuote(QuoteRequest request) {
        
        Zone pickupZone = zoneRepository.findByDistrict(request.getPickupDistrict())
                .orElseThrow(() -> new NotFoundException("Pickup zone not found for district: " + request.getPickupDistrict()));

        Zone dropoffZone = zoneRepository.findByDistrict(request.getDropoffDistrict())
                .orElseThrow(() -> new NotFoundException("Dropoff zone not found for district: " + request.getDropoffDistrict()));

        PricingRule rule = pricingRuleRepository.findByZoneId(pickupZone.getId())
                .orElseThrow(() -> new NotFoundException("Pricing rule not found for zone: " + pickupZone.getName()));

        boolean isInterZone = !pickupZone.getId().equals(dropoffZone.getId());

        PricingStrategy strategy = strategies.getOrDefault(activeStrategyName.toUpperCase(), strategies.get("STANDARD"));

        var fee = strategy.calculateFee(rule, request.getSize(), request.getWeightKg(), isInterZone);

        return QuoteResponse.builder()
                .fee(fee)
                .pickupZone(pickupZone.getName())
                .dropoffZone(dropoffZone.getName())
                .pickupZoneId(pickupZone.getId())
                .dropoffZoneId(dropoffZone.getId())
                .isInterZone(isInterZone)
                .build();
    }
}
