package com.swiftdrop.tracking.repository;

import com.swiftdrop.tracking.domain.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByParcelIdOrderByChangedAtAsc(Long parcelId);
}