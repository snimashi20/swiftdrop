package com.swiftdrop.pricing.repository;

import com.swiftdrop.pricing.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByDistrict(String district);
}
