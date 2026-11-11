package com.swiftdrop.courier;

import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.courier.domain.DeliveryProof;
import com.swiftdrop.courier.domain.FailureReason;
import com.swiftdrop.courier.dto.CourierDeliveryResponse;
import com.swiftdrop.courier.dto.DeliverParcelRequest;
import com.swiftdrop.courier.dto.FailParcelRequest;
import com.swiftdrop.courier.dto.UpdateStatusRequest;
import com.swiftdrop.courier.repository.DeliveryProofRepository;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.courier.service.FileStorageService;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.service.InvalidStatusTransitionException;
import com.swiftdrop.tracking.service.ParcelStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private ParcelRepository parcelRepository;

    @Mock
    private DeliveryProofRepository deliveryProofRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ParcelStatusService parcelStatusService;

    @InjectMocks
    private CourierService courierService;

    private Parcel parcel;

    @BeforeEach
    void setUp() {
        Address dropoff = new Address();
        dropoff.setLine1("456 Galle Rd");
        dropoff.setCity("Colombo");
        dropoff.setDistrict("Colombo");

        parcel = new Parcel();
        parcel.setId(99L);
        parcel.setTrackingNumber("SDTEST000099");
        parcel.setReceiverName("Kamal Perera");
        parcel.setReceiverPhone("0771234567");
        parcel.setDropoff(dropoff);
        parcel.setWeightKg(new BigDecimal("2.00"));
        parcel.setStatus(ParcelStatus.CREATED);
        parcel.setAssignedCourierId(101L);
    }

    @Test
    @DisplayName("getTodayDeliveries queries only the authenticated courier's profile ID")
    void getTodayDeliveries_queriesOnlyOwnCourierId() {
        when(parcelRepository.findTodayDeliveriesForCourier(eq(101L), any())).thenReturn(List.of(parcel));
        when(deliveryProofRepository.findByParcelId(99L)).thenReturn(Optional.empty());

        List<CourierDeliveryResponse> results = courierService.getTodayDeliveries(101L);

        assertEquals(1, results.size());
        assertEquals("SDTEST000099", results.get(0).getTrackingNumber());
        verify(parcelRepository).findTodayDeliveriesForCourier(eq(101L), any());
    }

    @Test
    @DisplayName("updateStatus normalizes lowercase status and calls 5-argument changeStatus with courierUserId")
    void updateStatus_lowercaseStatus_passesUserIdToFiveArgChangeStatus() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.of(parcel));
        when(deliveryProofRepository.findByParcelId(99L)).thenReturn(Optional.empty());

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("picked_up");
        req.setNote("Picked up from merchant");

        CourierDeliveryResponse response = courierService.updateStatus(101L, 55L, 99L, req);

        assertEquals("PICKED_UP", response.getStatus());
        verify(parcelStatusService).changeStatus(99L, ParcelStatus.PICKED_UP, "COURIER", 55L, "Picked up from merchant");
    }

    @Test
    @DisplayName("updateStatus rejects unknown status string with BadRequestException")
    void updateStatus_unknownStatus_throwsBadRequest() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.of(parcel));

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("NOT_A_REAL_STATUS");

        assertThrows(BadRequestException.class, () -> courierService.updateStatus(101L, 55L, 99L, req));
        verifyNoInteractions(parcelStatusService);
    }

    @Test
    @DisplayName("updateStatus propagates InvalidStatusTransitionException when transition is invalid")
    void updateStatus_invalidTransition_throwsInvalidStatusTransitionException() {
        parcel.setStatus(ParcelStatus.DELIVERED);
        when(parcelRepository.findById(99L)).thenReturn(Optional.of(parcel));
        doThrow(new InvalidStatusTransitionException(ParcelStatus.DELIVERED, ParcelStatus.PICKED_UP))
                .when(parcelStatusService)
                .changeStatus(99L, ParcelStatus.PICKED_UP, "COURIER", 55L, "Invalid retry");

        UpdateStatusRequest req = new UpdateStatusRequest();
        req.setStatus("PICKED_UP");
        req.setNote("Invalid retry");

        assertThrows(InvalidStatusTransitionException.class,
                () -> courierService.updateStatus(101L, 55L, 99L, req));
    }

    @Test
    @DisplayName("deliverParcel stores proof, saves record, and calls 5-argument changeStatus with courierUserId")
    void deliverParcel_validRequest_savesProofAndRecordsUserId() {
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        MockMultipartFile photo = new MockMultipartFile("photo", "proof.jpg", "image/jpeg", "img".getBytes());

        when(parcelRepository.findById(99L)).thenReturn(Optional.of(parcel));
        when(fileStorageService.store(photo)).thenReturn("/uploads/proofs/abc.jpg");
        when(deliveryProofRepository.findByParcelId(99L)).thenReturn(Optional.empty());

        DeliverParcelRequest req = new DeliverParcelRequest();
        req.setRecipientName("Mrs. Silva");
        req.setNote("Left at reception");

        CourierDeliveryResponse response = courierService.deliverParcel(101L, 55L, 99L, req, photo);

        assertEquals("DELIVERED", response.getStatus());
        verify(deliveryProofRepository).save(any(DeliveryProof.class));
        verify(parcelStatusService).changeStatus(99L, ParcelStatus.DELIVERED, "COURIER", 55L, "Delivered to Mrs. Silva");
    }

    @Test
    @DisplayName("failParcel saves failure details and calls 5-argument changeStatus with courierUserId")
    void failParcel_validRequest_savesFailureAndRecordsUserId() {
        parcel.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        when(parcelRepository.findById(99L)).thenReturn(Optional.of(parcel));
        when(deliveryProofRepository.findByParcelId(99L)).thenReturn(Optional.empty());

        FailParcelRequest req = new FailParcelRequest();
        req.setReason(FailureReason.RECIPIENT_NOT_AVAILABLE);
        req.setNote("Gate locked");

        CourierDeliveryResponse response = courierService.failParcel(101L, 55L, 99L, req);

        assertEquals("FAILED", response.getStatus());
        verify(parcelRepository).save(parcel);
        verify(parcelStatusService).changeStatus(
                99L, ParcelStatus.FAILED, "COURIER", 55L, "Failed: RECIPIENT_NOT_AVAILABLE - Gate locked");
    }
}