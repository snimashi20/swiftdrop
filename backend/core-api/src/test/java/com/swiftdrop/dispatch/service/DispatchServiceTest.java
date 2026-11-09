package com.swiftdrop.dispatch.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.domain.VehicleType;
import com.swiftdrop.dispatch.dto.CourierWorkloadResponse;
import com.swiftdrop.dispatch.dto.ParcelAssignmentResponse;
import com.swiftdrop.dispatch.dto.UnassignedParcelResponse;
import com.swiftdrop.dispatch.repository.CourierLoad;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.dispatch.repository.DispatchParcelRepository;
import com.swiftdrop.parcel.domain.Address;
import com.swiftdrop.parcel.domain.Parcel;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock DispatchParcelRepository parcelRepository;
    @Mock CourierProfileRepository courierRepository;
    @Mock UserRepository userRepository;
    @Mock ZoneLookup zoneLookup;
    @InjectMocks DispatchService service;

    private static Parcel parcel(long id, ParcelStatus status) {
        Parcel p = new Parcel();
        p.setId(id);
        p.setStatus(status);
        p.setTrackingNumber("SD0000000001");
        return p;
    }

    private static CourierProfile courier(long id, boolean active) {
        return CourierProfile.builder().id(id).userId(100 + id).vehicleType(VehicleType.MOTORBIKE)
                .zoneId(1L).active(active).build();
    }

    private static CourierLoad load(long courierId, long count) {
        return new CourierLoad() {
            public Long getCourierId() { return courierId; }
            public Long getActiveCount() { return count; }
        };
    }

    // ---------- assign ----------

    @Test
    void assign_setsCourierDispatcherAndTime_withoutChangingStatus() {
        Parcel p = parcel(5L, ParcelStatus.CREATED);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(p));
        when(courierRepository.findById(7L)).thenReturn(Optional.of(courier(7L, true)));
        when(parcelRepository.save(p)).thenReturn(p);

        ParcelAssignmentResponse result = service.assign(5L, 7L, 42L);

        assertEquals(7L, p.getAssignedCourierId());
        assertEquals(42L, p.getAssignedByUserId());
        assertNotNull(p.getAssignedAt());
        assertEquals(ParcelStatus.CREATED, p.getStatus(), "assigning must never change the status");
        assertEquals(7L, result.assignedCourierId());
    }

    @Test
    void assign_replacesCourier_whenParcelAlreadyAssigned() {
        Parcel p = parcel(5L, ParcelStatus.PICKED_UP);
        p.setAssignedCourierId(3L);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(p));
        when(courierRepository.findById(7L)).thenReturn(Optional.of(courier(7L, true)));
        when(parcelRepository.save(p)).thenReturn(p);

        service.assign(5L, 7L, 42L);

        assertEquals(7L, p.getAssignedCourierId());
    }

    @Test
    void assign_throwsNotFound_whenParcelMissing() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.assign(99L, 7L, 42L));
        verifyNoInteractions(courierRepository);
    }

    @Test
    void assign_throwsConflict_whenParcelAlreadyDelivered() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel(5L, ParcelStatus.DELIVERED)));

        assertThrows(ConflictException.class, () -> service.assign(5L, 7L, 42L));
        verify(parcelRepository, never()).save(any());
    }

    @Test
    void assign_throwsConflict_whenParcelFailed() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel(5L, ParcelStatus.FAILED)));

        assertThrows(ConflictException.class, () -> service.assign(5L, 7L, 42L));
    }

    @Test
    void assign_throwsNotFound_whenCourierMissing() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel(5L, ParcelStatus.CREATED)));
        when(courierRepository.findById(7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.assign(5L, 7L, 42L));
        verify(parcelRepository, never()).save(any());
    }

    @Test
    void assign_throwsBadRequest_whenCourierInactive() {
        Parcel p = parcel(5L, ParcelStatus.CREATED);
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(p));
        when(courierRepository.findById(7L)).thenReturn(Optional.of(courier(7L, false)));

        assertThrows(BadRequestException.class, () -> service.assign(5L, 7L, 42L));
        assertNull(p.getAssignedCourierId(), "nothing may be saved for an inactive courier");
        verify(parcelRepository, never()).save(any());
    }

    // ---------- logged-in user ----------

    @Test
    void resolveUserId_returnsIdOfUserWithThatEmail() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(userRepository.findByEmail("dispatcher@test.com")).thenReturn(Optional.of(user));

        assertEquals(42L, service.resolveUserId("dispatcher@test.com"));
    }

    @Test
    void resolveUserId_returnsNull_whenUserUnknownOrEmailMissing() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertNull(service.resolveUserId("nobody@test.com"));
        assertNull(service.resolveUserId(null));
        assertNull(service.resolveUserId("  "));
    }

    // ---------- workload ----------

    @Test
    void workload_listsLeastBusyFirst_andCountsZeroForIdleCouriers() {
        when(courierRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(courier(1L, true), courier(2L, true))));
        when(parcelRepository.countActiveByCourier(any())).thenReturn(List.of(load(1L, 3L)));

        List<CourierWorkloadResponse> result = service.workload(null, null);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).courierProfileId());
        assertEquals(0, result.get(0).activeDeliveries());
        assertEquals(1L, result.get(1).courierProfileId());
        assertEquals(3, result.get(1).activeDeliveries());
    }

    @Test
    void workload_filtersByZoneAndActive() {
        when(courierRepository.findByZoneIdAndActive(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(courier(1L, true))));
        when(parcelRepository.countActiveByCourier(any())).thenReturn(List.of());

        List<CourierWorkloadResponse> result = service.workload(1L, true);

        assertEquals(1, result.size());
        verify(courierRepository).findByZoneIdAndActive(eq(1L), eq(true), any(Pageable.class));
    }

    // ---------- unassigned list ----------

    @Test
    @SuppressWarnings("unchecked")
    void listUnassigned_queriesRepositoryWithPaging() {
        Pageable pageable = PageRequest.of(0, 20);
        when(parcelRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        Page<UnassignedParcelResponse> result = service.listUnassigned(null, "Colombo", null, null, pageable);

        assertTrue(result.isEmpty());
        verify(parcelRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUnassigned_turnsZoneIdIntoDistrict() {
        Pageable pageable = PageRequest.of(0, 20);
        when(zoneLookup.districtOf(3L)).thenReturn(Optional.of("Colombo"));
        when(parcelRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        service.listUnassigned(3L, null, null, null, pageable);

        verify(zoneLookup).districtOf(3L);
        verify(parcelRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void listUnassigned_throwsNotFound_whenZoneUnknown() {
        when(zoneLookup.districtOf(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.listUnassigned(99L, null, null, null, PageRequest.of(0, 20)));
        verifyNoInteractions(parcelRepository);
    }

    // ---------- suggested courier ----------

    private static Parcel parcelPickedUpIn(String district) {
        Parcel p = parcel(5L, ParcelStatus.CREATED);
        Address pickup = new Address();
        pickup.setDistrict(district);
        p.setPickup(pickup);
        return p;
    }

    @Test
    void suggestCourier_returnsLeastBusyActiveCourierInZone() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcelPickedUpIn("Colombo")));
        when(zoneLookup.zoneIdOfDistrict("Colombo")).thenReturn(Optional.of(1L));
        when(courierRepository.findByZoneIdAndActive(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(courier(1L, true), courier(2L, true))));
        when(parcelRepository.countActiveByCourier(any())).thenReturn(List.of(load(1L, 4L), load(2L, 1L)));

        CourierWorkloadResponse result = service.suggestCourier(5L);

        assertEquals(2L, result.courierProfileId());
        assertEquals(1, result.activeDeliveries());
    }

    @Test
    void suggestCourier_throwsNotFound_whenNoZoneCoversThePickupDistrict() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcelPickedUpIn("Atlantis")));
        when(zoneLookup.zoneIdOfDistrict("Atlantis")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.suggestCourier(5L));
    }

    @Test
    void suggestCourier_throwsNotFound_whenNoActiveCourierInZone() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcelPickedUpIn("Colombo")));
        when(zoneLookup.zoneIdOfDistrict("Colombo")).thenReturn(Optional.of(1L));
        when(courierRepository.findByZoneIdAndActive(eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(parcelRepository.countActiveByCourier(any())).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> service.suggestCourier(5L));
    }

    @Test
    void suggestCourier_throwsConflict_whenParcelAlreadyFinished() {
        when(parcelRepository.findById(5L)).thenReturn(Optional.of(parcel(5L, ParcelStatus.DELIVERED)));

        assertThrows(ConflictException.class, () -> service.suggestCourier(5L));
        verifyNoInteractions(zoneLookup);
    }

    @Test
    void suggestCourier_throwsNotFound_whenParcelMissing() {
        when(parcelRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.suggestCourier(99L));
    }

    @Test
    void listUnassigned_throwsBadRequest_whenEndDateIsBeforeStartDate() {
        assertThrows(BadRequestException.class, () -> service.listUnassigned(
                null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9), PageRequest.of(0, 20)));
        verifyNoInteractions(parcelRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void listUnassigned_acceptsTheSameStartAndEndDate() {
        Pageable pageable = PageRequest.of(0, 20);
        when(parcelRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(Page.empty());

        service.listUnassigned(null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10), pageable);

        verify(parcelRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ---------- terminal statuses ----------

    @Test
    void terminalStatuses_includeDeliveredAndFailed_butNotActiveOnes() {
        assertTrue(TerminalStatuses.isTerminal(ParcelStatus.DELIVERED));
        assertTrue(TerminalStatuses.isTerminal(ParcelStatus.FAILED));
        assertFalse(TerminalStatuses.isTerminal(ParcelStatus.CREATED));
        assertFalse(TerminalStatuses.isTerminal(ParcelStatus.OUT_FOR_DELIVERY));
        assertFalse(TerminalStatuses.isTerminal(null));
    }
}