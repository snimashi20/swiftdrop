package com.swiftdrop.merchant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.common.exception.ValidationException;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantOrder;
import com.swiftdrop.merchant.dto.MerchantOrderMapper;
import com.swiftdrop.merchant.dto.MerchantOrderRequest;
import com.swiftdrop.merchant.dto.MerchantOrderResponse;
import com.swiftdrop.merchant.repository.MerchantOrderRepository;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.dto.ParcelResponse;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.parcel.service.ParcelService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceTest {
    @Mock ParcelService parcelService;
    @Mock MerchantRepository merchantRepository;
    @Mock MerchantOrderRepository orderRepository;
    @Mock ParcelRepository parcelRepository;
    @Mock MerchantOrderMapper mapper;

    private MerchantOrderWriter writer;
    private MerchantOrderService service;
    private final Merchant merchant = Merchant.builder().id(7L).build();
    private final MerchantOrder order = MerchantOrder.builder()
            .id(9L).merchant(merchant).parcelId(11L).idempotencyKey("key").build();
    private final Parcel parcel = parcel();
    private final MerchantOrderResponse response = MerchantOrderResponse.builder()
            .trackingNumber("SDABCDEFGHIJ").status(ParcelStatus.CREATED)
            .fee(new BigDecimal("100.00")).build();

    @BeforeEach
    void setUp() {
        writer = new MerchantOrderWriter(parcelService, merchantRepository, orderRepository);
        service = new MerchantOrderService(writer, orderRepository, parcelRepository, mapper);
    }

    @Test
    void newOrderIsCreated() {
        when(parcelService.createForMerchant(eq(7L), any())).thenReturn(parcelResponse());
        when(merchantRepository.findById(7L)).thenReturn(Optional.of(merchant));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(parcelRepository.findById(11L)).thenReturn(Optional.of(parcel));
        when(mapper.toResponse(any(), any())).thenReturn(response);

        MerchantOrderService.CreationResult result = service.create(7L, "key", request());

        assertTrue(result.created());
        assertEquals("SDABCDEFGHIJ", result.response().getTrackingNumber());
        verify(orderRepository).save(any(MerchantOrder.class));
    }

    @Test
    void duplicateKeyReturnsOriginalAndCreatesNothing() {
        when(parcelService.createForMerchant(eq(7L), any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(merchantRepository.findById(7L)).thenReturn(Optional.of(merchant));
        when(orderRepository.findByMerchantIdAndIdempotencyKey(7L, "key")).thenReturn(Optional.of(order));
        when(parcelRepository.findById(11L)).thenReturn(Optional.of(parcel));
        when(mapper.toResponse(any(), any())).thenReturn(response);

        MerchantOrderService.CreationResult result = service.create(7L, "key", request());

        assertFalse(result.created());
        assertEquals("SDABCDEFGHIJ", result.response().getTrackingNumber());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownDistrictIsValidationFailureAndSavesNoOrder() {
        when(merchantRepository.findById(7L)).thenReturn(Optional.of(merchant));
        when(parcelService.createForMerchant(eq(7L), any()))
                .thenThrow(new NotFoundException("Pickup zone not found"));

        assertThrows(ValidationException.class, () -> service.create(7L, "key", request()));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void bulkFailureDoesNotSaveOrderAfterThirdItemFails() {
        when(parcelService.createForMerchant(eq(7L), any()))
                .thenReturn(parcelResponse(), parcelResponse())
                .thenThrow(new NotFoundException("Dropoff zone not found"));
        when(merchantRepository.findById(7L)).thenReturn(Optional.of(merchant));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ValidationException.class,
                () -> service.createBulk(7L, "batch", List.of(request(), request(), request())));
        verify(orderRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void anotherMerchantsTrackingNumberIsNotFound() {
        when(orderRepository.findByMerchantIdAndParcelTrackingNumber(7L, "SDOTHER1234"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(7L, "SDOTHER1234"));
    }

    @Test
    void missingMerchantIsNotConvertedToDistrictError() {
        when(merchantRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(7L, "key", request()));
        verify(parcelService, never()).createForMerchant(any(), any());
    }

    private MerchantOrderRequest request() {
        MerchantOrderRequest request = new MerchantOrderRequest();
        request.setPickup(new com.swiftdrop.parcel.dto.ParcelAddressDto());
        request.getPickup().setLine1("pickup");
        request.getPickup().setCity("city");
        request.getPickup().setDistrict("district");
        request.setDropoff(new com.swiftdrop.parcel.dto.ParcelAddressDto());
        request.getDropoff().setLine1("dropoff");
        request.getDropoff().setCity("city");
        request.getDropoff().setDistrict("district");
        request.setReceiverName("receiver");
        request.setReceiverPhone("0771234567");
        request.setSize(ParcelSize.M);
        request.setWeightKg(BigDecimal.ONE);
        return request;
    }

    private ParcelResponse parcelResponse() {
        return ParcelResponse.builder().id(11L).trackingNumber("SDABCDEFGHIJ")
                .status(ParcelStatus.CREATED).fee(new BigDecimal("100.00")).build();
    }

    private Parcel parcel() {
        Parcel parcel = new Parcel();
        parcel.setId(11L);
        parcel.setTrackingNumber("SDABCDEFGHIJ");
        parcel.setStatus(ParcelStatus.CREATED);
        parcel.setFee(new BigDecimal("100.00"));
        return parcel;
    }
}
