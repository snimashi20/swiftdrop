package com.swiftdrop.parcel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.ParcelDetailResponse;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.pricing.service.PricingService;
import com.swiftdrop.tracking.service.ParcelStatusService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ParcelServiceQueryTest {

    private ParcelRepository parcelRepository;
    private ParcelService parcelService;

    @BeforeEach
    void setUp() {
        parcelRepository = mock(ParcelRepository.class);
        parcelService = new ParcelService(parcelRepository, mock(PricingService.class),
                mock(TrackingNumberGenerator.class), mock(ParcelStatusService.class));
    }

    private Parcel parcel(Long id, Long customerId) {
        Address address = new Address();
        address.setLine1("12 Galle Road");
        address.setCity("Colombo");
        address.setDistrict("Colombo");

        Parcel parcel = new Parcel();
        parcel.setId(id);
        parcel.setCustomerId(customerId);
        parcel.setTrackingNumber("SDABCDEFGHIJ");
        parcel.setStatus(ParcelStatus.CREATED);
        parcel.setPickup(address);
        parcel.setDropoff(address);
        parcel.setReceiverName("Nimal Perera");
        parcel.setReceiverPhone("0771234567");
        parcel.setSize(ParcelSize.M);
        parcel.setWeightKg(new BigDecimal("2.0"));
        parcel.setFee(new BigDecimal("1000.00"));
        return parcel;
    }

    @Test
    void listForCustomer_withoutStatus_returnsAllOfMyParcels() {
        Pageable pageable = PageRequest.of(0, 10);
        when(parcelRepository.findByCustomerId(7L, pageable))
                .thenReturn(new PageImpl<>(List.of(parcel(1L, 7L))));

        Page<ParcelDetailResponse> result = parcelService.listForCustomer(7L, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("SDABCDEFGHIJ", result.getContent().get(0).getTrackingNumber());
    }

    @Test
    void listForCustomer_withStatus_usesTheStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(parcelRepository.findByCustomerIdAndStatus(7L, ParcelStatus.CANCELLED, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<ParcelDetailResponse> result =
                parcelService.listForCustomer(7L, ParcelStatus.CANCELLED, pageable);

        assertEquals(0, result.getTotalElements());
        verify(parcelRepository).findByCustomerIdAndStatus(7L, ParcelStatus.CANCELLED, pageable);
    }

    @Test
    void getForCustomer_ownParcel_returnsDetail() {
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel(1L, 7L)));

        ParcelDetailResponse detail = parcelService.getForCustomer(7L, 1L);

        assertEquals(1L, detail.getId());
        assertEquals("Colombo", detail.getDropoff().getDistrict());
        assertEquals(new BigDecimal("1000.00"), detail.getFee());
    }

    @Test
    void getForCustomer_anotherCustomersParcel_isNotFound() {
        when(parcelRepository.findById(1L)).thenReturn(Optional.of(parcel(1L, 7L)));

        assertThrows(NotFoundException.class, () -> parcelService.getForCustomer(8L, 1L));
    }

    @Test
    void getForCustomer_unknownId_isNotFound() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> parcelService.getForCustomer(7L, 99L));
    }
}