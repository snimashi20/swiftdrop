package com.swiftdrop.dispatch.repository;

import com.swiftdrop.dispatch.domain.CourierProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourierProfileRepository extends JpaRepository<CourierProfile, Long> {

    boolean existsByUserId(Long userId);

    Optional<CourierProfile> findByUserId(Long userId);

    Page<CourierProfile> findByZoneId(Long zoneId, Pageable pageable);

    Page<CourierProfile> findByActive(boolean active, Pageable pageable);

    Page<CourierProfile> findByZoneIdAndActive(Long zoneId, boolean active, Pageable pageable);
}