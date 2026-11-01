package com.swiftdrop.dispatch.service;

import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.domain.VehicleType;
import com.swiftdrop.dispatch.dto.CourierProfileCreateRequest;
import com.swiftdrop.dispatch.dto.CourierProfileMapper;
import com.swiftdrop.dispatch.dto.CourierProfileResponse;
import com.swiftdrop.dispatch.dto.CourierProfileUpdateRequest;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierProfileServiceTest {

    @Mock CourierProfileRepository repository;
    @Mock CourierProfileMapper mapper;
    @InjectMocks CourierProfileService service;

    private static CourierProfileResponse response() {
        return new CourierProfileResponse(1L, 10L, VehicleType.MOTORBIKE, 3L, true, Instant.now(), Instant.now());
    }

    private static CourierProfile profile() {
        return CourierProfile.builder()
                .id(1L).userId(10L).vehicleType(VehicleType.MOTORBIKE).zoneId(3L).active(true).build();
    }

    @Test
    void create_savesActiveProfile_whenUserHasNoProfile() {
        CourierProfile saved = profile();
        when(repository.existsByUserId(10L)).thenReturn(false);
        when(repository.save(any(CourierProfile.class))).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response());

        CourierProfileResponse result = service.create(
                new CourierProfileCreateRequest(10L, VehicleType.MOTORBIKE, 3L, null));

        ArgumentCaptor<CourierProfile> captor = ArgumentCaptor.forClass(CourierProfile.class);
        verify(repository).save(captor.capture());
        assertTrue(captor.getValue().isActive(), "new profile defaults to active");
        assertEquals(10L, captor.getValue().getUserId());
        assertNotNull(result);
    }

    @Test
    void create_throwsConflict_whenProfileAlreadyExists() {
        when(repository.existsByUserId(10L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(
                new CourierProfileCreateRequest(10L, VehicleType.CAR, 3L, true)));
        verify(repository, never()).save(any());
    }

    @Test
    void get_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(99L));
    }

    @Test
    void update_changesOnlyProvidedFields() {
        CourierProfile existing = profile();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(response());

        service.update(1L, new CourierProfileUpdateRequest(null, null, false));

        assertFalse(existing.isActive());
        assertEquals(3L, existing.getZoneId(), "zone must stay unchanged");
        assertEquals(VehicleType.MOTORBIKE, existing.getVehicleType(), "vehicle must stay unchanged");
    }

    @Test
    void update_changesZoneAndVehicle() {
        CourierProfile existing = profile();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(response());

        service.update(1L, new CourierProfileUpdateRequest(VehicleType.VAN, 5L, null));

        assertEquals(VehicleType.VAN, existing.getVehicleType());
        assertEquals(5L, existing.getZoneId());
        assertTrue(existing.isActive(), "active must stay unchanged");
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.update(99L, new CourierProfileUpdateRequest(null, null, true)));
    }

    @Test
    void list_filtersByZoneAndActive() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByZoneIdAndActive(3L, true, pageable)).thenReturn(Page.empty());

        service.list(3L, true, pageable);

        verify(repository).findByZoneIdAndActive(3L, true, pageable);
    }

    @Test
    void list_withoutFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(Page.empty());

        service.list(null, null, pageable);

        verify(repository).findAll(pageable);
    }
}