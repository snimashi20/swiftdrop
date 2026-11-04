package com.swiftdrop.parcel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.CreateParcelRequest;
import com.swiftdrop.parcel.dto.ParcelAddressDto;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.pricing.dto.QuoteRequest;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.service.PricingService;
import com.swiftdrop.tracking.service.ParcelStatusService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ParcelServiceTest {

    private ParcelRepository parcelRepository;
    private PricingService pricingService;
    private TrackingNumberGenerator trackingNumberGenerator;
    private ParcelStatusService parcelStatusService;
    private ParcelService parcelService;

    @BeforeEach
    void setUp() {
        parcelRepository = mock(ParcelRepository.class);
        pricingService = mock(PricingService.class);
        trackingNumberGenerator = mock(TrackingNumberGenerator.class);
        parcelStatusService = mock(ParcelStatusService.class);
        parcelService = new ParcelService(parcelRepository, pricingService,
                trackingNumberGenerator, parcelStatusService);

        QuoteResponse quote = QuoteResponse.builder()
                .fee(new BigDecimal("1000.00"))
                .pickupZone("Colombo").dropoffZone("Kandy")
                .pickupZoneId(1L).dropoffZoneId(2L)
                .build();
        when(pricingService.calculateQuote(any(QuoteRequest.class))).thenReturn(quote);
        when(trackingNumberGenerator.generate()).thenReturn("SDABCDEFGHIJ");
        when(parcelRepository.save(any(Parcel.class))).thenAnswer(invocation -> {
            Parcel parcel = invocation.getArgument(0);
            parcel.setId(10L);
            return parcel;
        });
    }

    private CreateParcelRequest request() {
        ParcelAddressDto pickup = new ParcelAddressDto();
        pickup.setLine1("12 Galle Road");
        pickup.setCity("Colombo");
        pickup.setDistrict("Colombo");

        ParcelAddressDto dropoff = new ParcelAddressDto();
        dropoff.setLine1("5 Temple Street");
        dropoff.setCity("Kandy");
        dropoff.setDistrict("Kandy");

        CreateParcelRequest request = new CreateParcelRequest();
        request.setPickup(pickup);
        request.setDropoff(dropoff);
        request.setReceiverName("Nimal Perera");
        request.setReceiverPhone("0771234567");
        request.setSize(ParcelSize.M);
        request.setWeightKg(new BigDecimal("2.0"));
        return request;
    }

    @Test
    void createForMerchant_savesParcelWithServerFeeAndRecordsCreated() {
        ParcelResponse response = parcelService.createForMerchant(5L, request());

        assertEquals(10L, response.getId());
        assertEquals("SDABCDEFGHIJ", response.getTrackingNumber());
        assertEquals(ParcelStatus.CREATED, response.getStatus());
        assertEquals(new BigDecimal("1000.00"), response.getFee());

        ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
        verify(parcelRepository).save(captor.capture());
        assertEquals(5L, captor.getValue().getMerchantId());
        assertNull(captor.getValue().getCustomerId());
        assertEquals(1L, captor.getValue().getPickupZoneId());

        verify(parcelStatusService).recordCreated(any(Parcel.class), eq("MERCHANT"), isNull());
    }

    @Test
    void createForCustomer_setsCustomerIdAndRecordsCreated() {
        parcelService.createForCustomer(7L, request());

        ArgumentCaptor<Parcel> captor = ArgumentCaptor.forClass(Parcel.class);
        verify(parcelRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getCustomerId());
        assertNull(captor.getValue().getMerchantId());

        verify(parcelStatusService).recordCreated(any(Parcel.class), eq("CUSTOMER"), eq(7L));
    }

    @Test
    void unknownDistrict_savesNothing() {
        when(pricingService.calculateQuote(any(QuoteRequest.class)))
                .thenThrow(new NotFoundException("Pickup zone not found for district: Nowhere"));

        assertThrows(NotFoundException.class, () -> parcelService.createForMerchant(5L, request()));

        verify(parcelRepository, never()).save(any(Parcel.class));
        verify(parcelStatusService, never()).recordCreated(any(Parcel.class), any(), any());
    }
}
