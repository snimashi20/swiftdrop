package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.domain.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    Optional<PricingRule> findByZoneId(Long zoneId);
}
