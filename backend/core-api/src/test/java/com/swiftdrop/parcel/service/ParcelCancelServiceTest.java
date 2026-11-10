package com.swiftdrop.parcel.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.service.ParcelStatusService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelCancelServiceTest {

    private ParcelRepository parcelRepository;
    private ParcelStatusService parcelStatusService;
    private ParcelCancelService cancelService;

    @BeforeEach
    void setUp() {
        parcelRepository = mock(ParcelRepository.class);
        parcelStatusService = mock(ParcelStatusService.class);
        cancelService = new ParcelCancelService(parcelRepository, parcelStatusService);
    }

    private Parcel parcel(Long customerId, ParcelStatus status) {
        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setCustomerId(customerId);
        parcel.setStatus(status);
        return parcel;
    }

    @Test
    void cancel_ownCreatedParcel_changesStatusToCancelled() {
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel(7L, ParcelStatus.CREATED)));

        cancelService.cancelForCustomer(7L, 1L);

        verify(parcelStatusService).changeStatus(1L, ParcelStatus.CANCELLED, "CUSTOMER", 7L,
                "Cancelled by customer");
    }

    @Test
    void cancel_anotherCustomersParcel_isNotFound() {
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel(7L, ParcelStatus.CREATED)));

        assertThrows(NotFoundException.class, () -> cancelService.cancelForCustomer(8L, 1L));

        verify(parcelStatusService, never())
                .changeStatus(anyLong(), any(ParcelStatus.class), anyString(), anyLong(), anyString());
    }

    @Test
    void cancel_parcelAlreadyPickedUp_isConflict() {
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel(7L, ParcelStatus.PICKED_UP)));

        assertThrows(ConflictException.class, () -> cancelService.cancelForCustomer(7L, 1L));

        verify(parcelStatusService, never())
                .changeStatus(anyLong(), any(ParcelStatus.class), anyString(), anyLong(), anyString());
    }

    @Test
    void cancel_unknownParcel_isNotFound() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cancelService.cancelForCustomer(7L, 99L));
    }
}