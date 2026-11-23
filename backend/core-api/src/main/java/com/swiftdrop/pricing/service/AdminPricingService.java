package com.swiftdrop.pricing.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.pricing.domain.PricingRule;
import com.swiftdrop.pricing.domain.Zone;
import com.swiftdrop.pricing.dto.PricingRuleRequest;
import com.swiftdrop.pricing.dto.PricingRuleResponse;
import com.swiftdrop.pricing.dto.ZoneRequest;
import com.swiftdrop.pricing.dto.ZoneResponse;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPricingService {

    private final PricingRuleRepository pricingRuleRepository;
    private final ZoneRepository zoneRepository;

    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
                .map(z -> ZoneResponse.builder()
                        .id(z.getId())
                        .name(z.getName())
                        .district(z.getDistrict())
                        .isActive(z.isActive())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ZoneResponse createZone(ZoneRequest request) {
        zoneRepository.findByDistrict(request.getDistrict()).ifPresent(existing -> {
            throw new ConflictException("Zone with district '" + request.getDistrict() + "' already exists");
        });

        Zone zone = Zone.builder()
                .name(request.getName())
                .district(request.getDistrict())
                .isActive(true)
                .build();
        Zone saved = zoneRepository.save(zone);

        // Use the same size factors as the DB seed data
        PricingRule defaultRule = PricingRule.builder()
                .zone(saved)
                .baseFee(new BigDecimal("200.00"))
                .perKgFee(new BigDecimal("20.00"))
                .interZoneSurcharge(new BigDecimal("10.00"))
                .sizeFactorS(new BigDecimal("1.00"))
                .sizeFactorM(new BigDecimal("1.50"))
                .sizeFactorL(new BigDecimal("2.00"))
                .build();
        pricingRuleRepository.save(defaultRule);

        return ZoneResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .district(saved.getDistrict())
                .isActive(saved.isActive())
                .build();
    }

    public List<PricingRuleResponse> getAllRules() {
        return pricingRuleRepository.findAllOrderedByZoneName().stream()
                .map(this::mapToRuleResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PricingRuleResponse updateRule(Long id, PricingRuleRequest request) {
        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pricing rule not found with id: " + id));

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
