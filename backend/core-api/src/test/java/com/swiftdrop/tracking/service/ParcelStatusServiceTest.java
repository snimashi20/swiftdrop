package com.swiftdrop.tracking.service;

import com.swiftdrop.common.event.ParcelStatusChangedEvent;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelStatusServiceTest {

    @Mock
    private ParcelRepository parcelRepository;
    @Mock
    private StatusHistoryRepository statusHistoryRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ParcelStatusService service;

    @BeforeEach
    void setUp() {
        service = new ParcelStatusService(
                parcelRepository, statusHistoryRepository, new StatusTransitionRules(), eventPublisher);
    }

    private Parcel parcel(ParcelStatus status) {
        Parcel parcel = new Parcel();
        parcel.setId(7L);
        parcel.setTrackingNumber("SD7K2M9QX4LP");
        parcel.setCustomerId(5L);
        parcel.setMerchantId(null);
        parcel.setStatus(status);
        return parcel;
    }

    @Test
    void validChangeUpdatesParcelSavesHistoryAndPublishesEvent() {
        Parcel parcel = parcel(ParcelStatus.CREATED);
        when(parcelRepository.findById(7L)).thenReturn(Optional.of(parcel));

        service.changeStatus(7L, ParcelStatus.PICKED_UP, "COURIER", 42L, "collected");

        assertEquals(ParcelStatus.PICKED_UP, parcel.getStatus());
        verify(parcelRepository).save(parcel);

        ArgumentCaptor<StatusHistory> history = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(history.capture());
        assertEquals(ParcelStatus.CREATED, history.getValue().getOldStatus());
        assertEquals(ParcelStatus.PICKED_UP, history.getValue().getNewStatus());
        assertEquals(42L, history.getValue().getChangedByUserId());
        assertEquals("COURIER", history.getValue().getChangedByRole());

        ArgumentCaptor<ParcelStatusChangedEvent> event = ArgumentCaptor.forClass(ParcelStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals("SD7K2M9QX4LP", event.getValue().trackingNumber());
        assertEquals("CREATED", event.getValue().oldStatus());
        assertEquals("PICKED_UP", event.getValue().newStatus());
        assertEquals(5L, event.getValue().customerId());
        assertNotNull(event.getValue().eventId());
        assertEquals(history.getValue().getChangedAt(), event.getValue().changedAt());
    }

    @Test
    void stringOverloadKeepsExistingCallersWorking() {
        Parcel parcel = parcel(ParcelStatus.CREATED);
        when(parcelRepository.findById(7L)).thenReturn(Optional.of(parcel));

        service.changeStatus(7L, "picked_up", "COURIER", "note");

        assertEquals(ParcelStatus.PICKED_UP, parcel.getStatus());
    }

    @Test
    void invalidTransitionIsRefusedAndNothingIsSavedOrPublished() {
        Parcel parcel = parcel(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(7L)).thenReturn(Optional.of(parcel));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.changeStatus(7L, ParcelStatus.IN_TRANSIT, "COURIER", 42L, null));

        assertEquals(ParcelStatus.DELIVERED, parcel.getStatus());
        verifyNoInteractions(statusHistoryRepository, eventPublisher);
    }

    @Test
    void unknownStatusTextIsABadRequestNotAServerError() {
        assertThrows(BadRequestException.class, () -> service.changeStatus(7L, "HELLO", "COURIER", null));
        assertThrows(BadRequestException.class, () -> service.changeStatus(7L, " ", "COURIER", null));
        assertThrows(BadRequestException.class, () -> service.changeStatus(7L, null, "COURIER", null));
    }

    @Test
    void unknownParcelIsNotFound() {
        when(parcelRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.changeStatus(9L, ParcelStatus.PICKED_UP, "COURIER", 42L, null));
    }

    @Test
    void recordCreatedWritesFirstRowAndPublishesEventWithNullOldStatus() {
        Parcel parcel = parcel(ParcelStatus.CREATED);

        service.recordCreated(parcel, "CUSTOMER", 5L);

        ArgumentCaptor<StatusHistory> history = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(history.capture());
        assertNull(history.getValue().getOldStatus());
        assertEquals(ParcelStatus.CREATED, history.getValue().getNewStatus());

        ArgumentCaptor<ParcelStatusChangedEvent> event = ArgumentCaptor.forClass(ParcelStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertNull(event.getValue().oldStatus());
        assertEquals("CREATED", event.getValue().newStatus());
    }

    @Test
    void recordCreatedNeedsASavedParcel() {
        Parcel parcel = parcel(ParcelStatus.CREATED);
        parcel.setId(null);

        assertThrows(IllegalStateException.class, () -> service.recordCreated(parcel, "CUSTOMER", 5L));
        verifyNoInteractions(statusHistoryRepository, eventPublisher);
    }
}
