package com.swiftdrop.courier;

import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.courier.dto.UpdateStatusRequest;
import com.swiftdrop.courier.repository.DeliveryProofRepository;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.courier.service.FileStorageService;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.service.ParcelStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CourierAuthorizationTest {

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

    private Parcel parcelAssignedToCourierB;

    @BeforeEach
    void setUp() {
        Long courierBId = 202L;

        Address pickup = new Address();
        pickup.setLine1("123 Main St");
        pickup.setCity("Colombo");
        pickup.setDistrict("Colombo");

        Address dropoff = new Address();
        dropoff.setLine1("456 Galle Rd");
        dropoff.setCity("Colombo");
        dropoff.setDistrict("Colombo");

        parcelAssignedToCourierB = new Parcel();
        parcelAssignedToCourierB.setId(99L);
        parcelAssignedToCourierB.setTrackingNumber("SDTEST999999");
        parcelAssignedToCourierB.setReceiverName("John Doe");
        parcelAssignedToCourierB.setReceiverPhone("0771234567");
        parcelAssignedToCourierB.setPickup(pickup);
        parcelAssignedToCourierB.setDropoff(dropoff);
        parcelAssignedToCourierB.setSize(ParcelSize.M);
        parcelAssignedToCourierB.setWeightKg(new BigDecimal("2.50"));
        parcelAssignedToCourierB.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
        parcelAssignedToCourierB.setAssignedCourierId(courierBId);
    }

    @Test
    @DisplayName("Courier A cannot update status of Courier B's parcel (Must throw ForbiddenException)")
    void courierCannotUpdateAnotherCouriersParcel() {
        Long courierAId = 101L; // Logged in as Courier A
        Long parcelId = 99L;

        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcelAssignedToCourierB));

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("DELIVERED");
        request.setNote("Courier A attempting unauthorized status update");

        // Assert that calling updateStatus throws ForbiddenException (HTTP 403)
        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> courierService.updateStatus(courierAId, parcelId, request)
        );

        assertEquals("Forbidden: You are not assigned to this parcel delivery", exception.getMessage());
    }
}