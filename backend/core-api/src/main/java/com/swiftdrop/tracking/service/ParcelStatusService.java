package com.swiftdrop.tracking.service;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * The only place where a parcel status is recorded or changed.
 * Every change saves a history row and publishes a {@link ParcelStatusChangedEvent};
 * listeners (RabbitMQ publisher, WebSocket broadcaster) react after the DB commit.
 */
@Service
@RequiredArgsConstructor
public class ParcelStatusService {

    private final ParcelRepository parcelRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final StatusTransitionRules statusTransitionRules;
    private final ApplicationEventPublisher eventPublisher;

    /** Kept so existing callers (CourierService) keep compiling. */
    @Transactional
    public void changeStatus(Long parcelId, String newStatus, String changedByRole, String note) {
        changeStatus(parcelId, parseStatus(newStatus), changedByRole, null, note);
    }

    @Transactional
    public void changeStatus(Long parcelId, ParcelStatus newStatus, String changedByRole,
                             Long changedByUserId, String note) {
        Parcel parcel = parcelRepository.findById(parcelId)
                .orElseThrow(() -> new NotFoundException("Parcel not found: " + parcelId));

        ParcelStatus oldStatus = parcel.getStatus();
        statusTransitionRules.validate(oldStatus, newStatus);

        parcel.setStatus(newStatus);
        parcelRepository.save(parcel);

        saveHistoryAndPublish(parcel, oldStatus, newStatus, changedByRole, changedByUserId, note, now());
    }

    /**
     * First history row (null -> CREATED) for a newly booked parcel.
     * Call it inside the booking transaction, right after the Parcel was saved.
     */
    @Transactional
    public void recordCreated(Parcel parcel, String changedByRole, Long changedByUserId) {
        if (parcel.getId() == null) {
            throw new IllegalStateException("The parcel must be saved before recording its creation");
        }
        if (parcel.getStatus() != ParcelStatus.CREATED) {
            throw new ConflictException("Only a parcel in status CREATED can be recorded as created");
        }
        saveHistoryAndPublish(parcel, null, ParcelStatus.CREATED, changedByRole, changedByUserId,
                "Booking confirmed", now());
    }

    private void saveHistoryAndPublish(Parcel parcel, ParcelStatus oldStatus, ParcelStatus newStatus,
                                       String changedByRole, Long changedByUserId, String note,
                                       Instant changedAt) {
        statusHistoryRepository.save(StatusHistory.builder()
                .parcelId(parcel.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedByUserId(changedByUserId)
                .changedByRole(changedByRole)
                .note(note)
                .changedAt(changedAt)
                .build());

        eventPublisher.publishEvent(new ParcelStatusChangedEvent(
                UUID.randomUUID(),
                parcel.getId(),
                parcel.getTrackingNumber(),
                oldStatus == null ? null : oldStatus.name(),
                newStatus.name(),
                changedAt,
                changedByRole,
                parcel.getCustomerId(),
                parcel.getMerchantId(),
                note));
    }

    private ParcelStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Status is required");
        }
        try {
            return ParcelStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown parcel status");
        }
    }

    // Millisecond precision so the REST timeline and the WebSocket message show identical times.
    private Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
