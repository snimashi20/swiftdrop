package com.swiftdrop.tracking.service;

import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import com.swiftdrop.parcel.repository.ParcelRepository;
import com.swiftdrop.tracking.domain.StatusHistory;
import com.swiftdrop.tracking.dto.TrackingResponse;
import com.swiftdrop.tracking.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingServiceTest {

    @Mock
    private ParcelRepository parcelRepository;
    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(parcelRepository, statusHistoryRepository);
    }

    private Address address(String city) {
        Address address = new Address();
        address.setLine1("12 Secret Road");
        address.setCity(city);
        address.setDistrict("Somewhere");
        return address;
    }

    private Parcel parcel() {
        Parcel parcel = new Parcel();
        parcel.setId(7L);
        parcel.setTrackingNumber("SD7K2M9QX4LP");
        parcel.setStatus(ParcelStatus.IN_TRANSIT);
        parcel.setReceiverName("Nimal Perera");
        parcel.setReceiverPhone("0771234567");
        parcel.setPickup(address("Colombo"));
        parcel.setDropoff(address("Kandy"));
        parcel.setCreatedAt(Instant.parse("2026-10-28T08:00:00Z"));
        return parcel;
    }

    private StatusHistory history(ParcelStatus status, String time) {
        return StatusHistory.builder().parcelId(7L).newStatus(status)
                .changedByRole("COURIER").note("Delivered to Nimal Perera")
                .changedAt(Instant.parse(time)).build();
    }

    @Test
    void returnsStatusTimelineCitiesAndShortNameOnly() {
        when(parcelRepository.findByTrackingNumber("SD7K2M9QX4LP")).thenReturn(Optional.of(parcel()));
        when(statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(7L)).thenReturn(List.of(
                history(ParcelStatus.CREATED, "2026-10-28T08:00:00Z"),
                history(ParcelStatus.IN_TRANSIT, "2026-10-28T13:40:00Z")));

        TrackingResponse response = service.getByTrackingNumber("sd7k2m9qx4lp");

        assertEquals("SD7K2M9QX4LP", response.trackingNumber());
        assertEquals(ParcelStatus.IN_TRANSIT, response.status());
        assertEquals("N. Perera", response.receiverName());
        assertEquals("Colombo", response.pickupCity());
        assertEquals("Kandy", response.dropoffCity());
        assertEquals(2, response.timeline().size());
        assertEquals(ParcelStatus.CREATED, response.timeline().get(0).status());
        // The response type has no note, phone or street address, so none can leak.
    }

    @Test
    void parcelWithoutHistoryStillShowsItsCurrentStatus() {
        when(parcelRepository.findByTrackingNumber("SD7K2M9QX4LP")).thenReturn(Optional.of(parcel()));
        when(statusHistoryRepository.findByParcelIdOrderByChangedAtAsc(7L)).thenReturn(List.of());

        TrackingResponse response = service.getByTrackingNumber("SD7K2M9QX4LP");

        assertEquals(1, response.timeline().size());
        assertEquals(ParcelStatus.IN_TRANSIT, response.timeline().get(0).status());
    }

    @Test
    void unknownTrackingNumberIsNotFound() {
        when(parcelRepository.findByTrackingNumber("SD0000000000")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByTrackingNumber("SD0000000000"));
    }

    @Test
    void malformedTrackingNumberIsNotFoundWithoutTouchingTheDatabase() {
        assertThrows(NotFoundException.class, () -> service.getByTrackingNumber("ABC"));
        assertThrows(NotFoundException.class, () -> service.getByTrackingNumber(null));
        verifyNoInteractions(parcelRepository, statusHistoryRepository);
    }

    @Test
    void shortNameKeepsFirstInitialAndLastName() {
        assertEquals("N. Perera", TrackingService.shortName("Nimal Perera"));
        assertEquals("N. Perera", TrackingService.shortName("  nimal kumara Perera "));
        assertEquals("N.", TrackingService.shortName("Nimal"));
        assertEquals("", TrackingService.shortName("  "));
        assertEquals("", TrackingService.shortName(null));
    }
}
