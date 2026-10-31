package com.swiftdrop.tracking.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ParcelStatusService {

    private final ParcelRepository parcelRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final StatusTransitionRules statusTransitionRules;

    @Transactional
    public void changeStatus(Long parcelId, String newStatusStr, String changedByRole, String note) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));

        ParcelStatus oldStatus = parcel.getStatus();
        ParcelStatus newStatus = ParcelStatus.valueOf(newStatusStr);

        // Validate transition using M5 rule engine
        statusTransitionRules.validate(oldStatus, newStatus);

        // Update status on parcel
        parcel.setStatus(newStatus);
        parcelRepository.save(parcel);

        // Record history entry
        StatusHistory history = StatusHistory.builder()
                .parcelId(parcelId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedByRole(changedByRole)
                .note(note)
                .changedAt(Instant.now())
                .build();

        statusHistoryRepository.save(history);
    }
}