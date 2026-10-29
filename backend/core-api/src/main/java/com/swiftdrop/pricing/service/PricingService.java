package com.swiftdrop.pricing.service;

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
    
    // This is the config switch M3 asked for!
    private final Map<String, PricingStrategy> strategies;

    @Value("${PRICING_STRATEGY:STANDARD}")
    private String activeStrategyName;

    public QuoteResponse calculateQuote(QuoteRequest request) {
        // 1. Find the pickup and dropoff zones
        Zone pickupZone = zoneRepository.findByDistrict(request.getPickupDistrict())
                .orElseThrow(() -> new RuntimeException("Pickup zone not found")); // Replace with M8's NotFoundException later
        Zone dropoffZone = zoneRepository.findByDistrict(request.getDropoffDistrict())
                .orElseThrow(() -> new RuntimeException("Dropoff zone not found"));

        // 2. Get the pricing rule for the pickup zone
        PricingRule rule = pricingRuleRepository.findByZoneId(pickupZone.getId())
                .orElseThrow(() -> new RuntimeException("Pricing rule not found"));

        // 3. Determine if it's inter-zone
        boolean isInterZone = !pickupZone.getId().equals(dropoffZone.getId());

        // 4. Select the strategy (STANDARD or EXPRESS)
        PricingStrategy strategy = strategies.getOrDefault(activeStrategyName.toUpperCase(), strategies.get("STANDARD"));

        // 5. Calculate the fee
        var fee = strategy.calculateFee(rule, request.getSize(), request.getWeightKg(), isInterZone);

        // 6. Return the response
        return QuoteResponse.builder()
                .fee(fee)
                .pickupZone(pickupZone.getName())
                .dropoffZone(dropoffZone.getName())
                .isInterZone(isInterZone)
                .build();
    }
}
