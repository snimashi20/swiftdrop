package com.swiftdrop.pricing.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.pricing.domain.PricingRule;
import com.swiftdrop.pricing.domain.Zone;
import com.swiftdrop.pricing.dto.*;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPricingService {

    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;

    // Zones
    
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(this::mapToZoneResponse)
                .toList();
    }

    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        // Check for duplicates
        if (zoneRepository.findByDistrict(request.getDistrict()).isPresent()) {
            throw new ConflictException("Zone already exists for district: " + request.getDistrict());
        }
        
        Zone zone = Zone.builder()
                .name(request.getName())
                .district(request.getDistrict())
                .build();
        Zone savedZone = zoneRepository.save(zone);
        
        // Create a default pricing rule so new zones can be quoted immediately
        PricingRule defaultRule = PricingRule.builder()
                .zone(savedZone)
                .baseFee(new BigDecimal("300.00"))
                .perKgFee(new BigDecimal("100.00"))
                .interZoneSurcharge(new BigDecimal("250.00"))
                .build();
        pricingRuleRepository.save(defaultRule);
        
        return mapToZoneResponse(savedZone);
    }

    private ZoneResponse mapToZoneResponse(Zone zone) {
        return ZoneResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .district(zone.getDistrict())
                .isActive(zone.isActive())
                .build();
    }

    // Pricing Rules

    public List<PricingRuleResponse> getAllRules() {
        return pricingRuleRepository.findAll().stream()
                .map(this::mapToRuleResponse)
                .toList();
    }

    @Transactional
    public PricingRuleResponse updateRule(Long id, PricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pricing rule not found with id: " + id));

        // Do NOT change the zone associated with the rule (prevents UNIQUE constraint violation)
        rule.setBaseFee(request.getBaseFee());
        rule.setPerKgFee(request.getPerKgFee());
        rule.setInterZoneSurcharge(request.getInterZoneSurcharge());

        if (request.getSizeFactorS() != null) rule.setSizeFactorS(request.getSizeFactorS());
        if (request.getSizeFactorM() != null) rule.setSizeFactorM(request.getSizeFactorM());
        if (request.getSizeFactorL() != null) rule.setSizeFactorL(request.getSizeFactorL());

        return mapToRuleResponse(pricingRuleRepository.save(rule));
    }

    private PricingRuleResponse mapToRuleResponse(PricingRule rule) {
        return PricingRuleResponse.builder()
                .id(rule.getId())
                .zoneId(rule.getZone().getId())
                .zoneName(rule.getZone().getName())
                .baseFee(rule.getBaseFee())
                .perKgFee(rule.getPerKgFee())
                .interZoneSurcharge(rule.getInterZoneSurcharge())
                .sizeFactorS(rule.getSizeFactorS())
                .sizeFactorM(rule.getSizeFactorM())
                .sizeFactorL(rule.getSizeFactorL())
                .build();
    }
}
