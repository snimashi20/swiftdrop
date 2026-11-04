package com.swiftdrop.pricing.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.pricing.domain.PricingRule;
import com.swiftdrop.pricing.domain.Zone;
import com.swiftdrop.pricing.dto.PricingRuleRequest;
import com.swiftdrop.pricing.dto.ZoneRequest;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.repository.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminPricingServiceTest {

    private AdminPricingService service;
    private ZoneRepository zoneRepository;
    private PricingRuleRepository ruleRepository;

    @BeforeEach
    void setUp() {
        zoneRepository = Mockito.mock(ZoneRepository.class);
        ruleRepository = Mockito.mock(PricingRuleRepository.class);
        service = new AdminPricingService(zoneRepository, ruleRepository);
    }

    @Test
    void createZone_success() {
        ZoneRequest request = new ZoneRequest();
        request.setName("Test Zone");
        request.setDistrict("Test District");

        Zone savedZone = Zone.builder().id(1L).name("Test Zone").district("Test District").build();
        when(zoneRepository.save(any(Zone.class))).thenReturn(savedZone);

        var response = service.createZone(request);
        assertEquals(1L, response.getId());
        assertEquals("Test Zone", response.getName());
        
        // Ensure default rule was created
        Mockito.verify(ruleRepository).save(any(PricingRule.class));
    }

    @Test
    void createZone_duplicateDistrict_throwsConflictException() {
        ZoneRequest request = new ZoneRequest();
        request.setName("Colombo Zone");
        request.setDistrict("Colombo");

        when(zoneRepository.findByDistrict("Colombo")).thenReturn(Optional.of(
            Zone.builder().id(1L).district("Colombo").build()
        ));

        assertThrows(ConflictException.class, () -> service.createZone(request));
    }

    @Test
    void updateRule_ruleNotFound_throwsException() {
        when(ruleRepository.findById(99L)).thenReturn(Optional.empty());
        
        PricingRuleRequest request = new PricingRuleRequest();
        request.setZoneId(1L);
        
        assertThrows(NotFoundException.class, () -> service.updateRule(99L, request));
    }

    @Test
    void updateRule_successAndDoesNotChangeZone() {
        Zone zone = Zone.builder().id(1L).name("Colombo").build();
        PricingRule rule = PricingRule.builder().id(10L).zone(zone)
                .baseFee(new BigDecimal("100")).perKgFee(new BigDecimal("10"))
                .interZoneSurcharge(new BigDecimal("50")).build();

        PricingRuleRequest request = new PricingRuleRequest();
        request.setZoneId(999L); // Attempting to change the zone
        request.setBaseFee(new BigDecimal("200"));
        request.setPerKgFee(new BigDecimal("20"));
        request.setInterZoneSurcharge(new BigDecimal("75"));

        when(ruleRepository.findById(10L)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any(PricingRule.class))).thenReturn(rule);

        var response = service.updateRule(10L, request);
        
        // Assertions
        assertEquals(new BigDecimal("200"), response.getBaseFee());
        assertEquals(1L, response.getZoneId()); // Zone should STILL be 1, not 999
    }
}
