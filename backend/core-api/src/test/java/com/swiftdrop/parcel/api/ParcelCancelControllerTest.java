package com.swiftdrop.parcel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.service.ParcelCancelService;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParcelCancelControllerTest {

    private ParcelCancelService parcelCancelService;
    private ParcelService parcelService;
    private ParcelCancelController controller;
    private final AuthenticatedUser customer = new AuthenticatedUser(7L, "customer@example.com", "CUSTOMER");

    @BeforeEach
    void setUp() {
        parcelCancelService = mock(ParcelCancelService.class);
        parcelService = mock(ParcelService.class);
        controller = new ParcelCancelController(parcelCancelService, parcelService);
    }

    @Test
    void cancel_cancelsTheLoggedInCustomersParcelAndReturnsTheUpdatedParcel() {
        ParcelDetailResponse updated = ParcelDetailResponse.builder()
                .id(1L).status(ParcelStatus.CANCELLED).build();
        when(parcelService.getForCustomer(7L, 1L)).thenReturn(updated);

        ParcelDetailResponse result = controller.cancel(1L, customer);

        verify(parcelCancelService).cancelForCustomer(7L, 1L);
        assertEquals(ParcelStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancel_parcelOfAnotherCustomer_isNotFoundAndNothingIsReturned() {
        doThrow(new NotFoundException("Parcel not found"))
                .when(parcelCancelService).cancelForCustomer(7L, 99L);

        assertThrows(NotFoundException.class, () -> controller.cancel(99L, customer));

        verify(parcelService, never()).getForCustomer(7L, 99L);
    }
}