package com.swiftdrop.pricing.api;

import com.swiftdrop.pricing.dto.*;
import com.swiftdrop.pricing.service.AdminPricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Secures ALL endpoints in this controller
public class AdminPricingController {

    private final AdminPricingService adminPricingService;

    // Zones
    @GetMapping("/zones")
    public ResponseEntity<List<ZoneResponse>> getAllZones() {
        return ResponseEntity.ok(adminPricingService.getAllZones());
    }

    @PostMapping("/zones")
    public ResponseEntity<ZoneResponse> createZone(@Valid @RequestBody ZoneRequest request) {
        ZoneResponse created = adminPricingService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Pricing Rules
    @GetMapping("/rules")
    public ResponseEntity<List<PricingRuleResponse>> getAllRules() {
        return ResponseEntity.ok(adminPricingService.getAllRules());
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<PricingRuleResponse> updateRule(
            @PathVariable Long id, 
            @Valid @RequestBody PricingRuleRequest request) {
        return ResponseEntity.ok(adminPricingService.updateRule(id, request));
    }
}
