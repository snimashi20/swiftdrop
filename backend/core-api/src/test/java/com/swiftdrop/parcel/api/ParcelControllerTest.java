package com.swiftdrop.parcel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.CreateParcelRequest;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.service.ParcelService;
import com.swiftdrop.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ParcelControllerTest {

    private final AuthenticatedUser customer = new AuthenticatedUser(7L, "customer@example.com", "CUSTOMER");

    private ParcelService parcelService;
    private ParcelController controller;

    @BeforeEach
    void setUp() {
        parcelService = mock(ParcelService.class);
        controller = new ParcelController(parcelService);
    }

    @Test
    void create_booksTheParcelForTheLoggedInCustomer() {
        CreateParcelRequest request = new CreateParcelRequest();
        ParcelResponse parcelResponse = ParcelResponse.builder()
                .id(10L).trackingNumber("SDABCDEFGHIJ").build();
        when(parcelService.createForCustomer(7L, request)).thenReturn(parcelResponse);

        ResponseEntity<ParcelResponse> result = controller.create(request, customer);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("SDABCDEFGHIJ", result.getBody().getTrackingNumber());
    }

    @Test
    void list_usesTheLoggedInCustomerAndKeepsPageAndSizeInRange() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(parcelService.listForCustomer(eq(7L), eq(ParcelStatus.CANCELLED), pageable.capture()))
                .thenReturn(Page.empty());

        controller.list(-5, 500, ParcelStatus.CANCELLED, customer);

        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(50, pageable.getValue().getPageSize());
    }

    @Test
    void get_returnsTheLoggedInCustomersParcel() {
        ParcelDetailResponse detail = ParcelDetailResponse.builder().id(3L).build();
        when(parcelService.getForCustomer(7L, 3L)).thenReturn(detail);

        ParcelDetailResponse result = controller.get(3L, customer);

        assertEquals(3L, result.getId());
    }
}