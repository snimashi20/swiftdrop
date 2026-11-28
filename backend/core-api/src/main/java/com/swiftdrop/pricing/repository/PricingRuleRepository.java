package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.domain.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {
    
    Optional<PricingRule> findByZoneId(Long zoneId);

    @Query("SELECT pr FROM PricingRule pr JOIN FETCH pr.zone z ORDER BY z.name ASC")
    List<PricingRule> findAllOrderedByZoneName();
}
