package com.swiftdrop.courier.repository;

import com.swiftdrop.courier.domain.CourierApplication;
import com.swiftdrop.courier.domain.CourierApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourierApplicationRepository extends JpaRepository<CourierApplication, Long> {

    Optional<CourierApplication> findByUserId(Long userId);

    boolean existsByContactEmail(String contactEmail);

    Page<CourierApplication> findByStatus(CourierApplicationStatus status, Pageable pageable);
}
