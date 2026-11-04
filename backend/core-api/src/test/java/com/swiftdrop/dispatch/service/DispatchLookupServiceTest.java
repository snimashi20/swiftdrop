package com.swiftdrop.dispatch.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.domain.VehicleType;
import com.swiftdrop.dispatch.dto.CourierCandidateResponse;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchLookupServiceTest {

    @Mock UserRepository userRepository;
    @Mock CourierProfileRepository courierRepository;
    @Mock ZoneLookup zoneLookup;
    @InjectMocks DispatchLookupService service;

    private static User courierUser(long id, String name, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setRole("COURIER");
        user.setEnabled(enabled);
        return user;
    }

    @Test
    void courierCandidates_listsEnabledCouriersWithoutProfile_sortedByName() {
        when(userRepository.findByRoleIgnoreCase("COURIER")).thenReturn(List.of(
                courierUser(1L, "Zara", true),
                courierUser(2L, "Amal", true),
                courierUser(3L, "Disabled Dan", false),
                courierUser(4L, "Has Profile", true)));
        when(courierRepository.findAll()).thenReturn(List.of(
                CourierProfile.builder().id(9L).userId(4L).vehicleType(VehicleType.CAR).zoneId(1L).build()));

        List<CourierCandidateResponse> result = service.courierCandidates();

        assertEquals(2, result.size());
        assertEquals("Amal", result.get(0).fullName());
        assertEquals(2L, result.get(0).userId());
        assertEquals("Zara", result.get(1).fullName());
    }

    @Test
    void courierCandidates_isEmpty_whenEveryCourierHasAProfile() {
        when(userRepository.findByRoleIgnoreCase("COURIER")).thenReturn(List.of(courierUser(4L, "Has Profile", true)));
        when(courierRepository.findAll()).thenReturn(List.of(
                CourierProfile.builder().id(9L).userId(4L).vehicleType(VehicleType.CAR).zoneId(1L).build()));

        assertTrue(service.courierCandidates().isEmpty());
    }

    @Test
    void zones_returnsWhatTheZoneLookupFinds() {
        List<ZoneLookup.ZoneOption> zones = List.of(new ZoneLookup.ZoneOption(1L, "Colombo Zone", "Colombo"));
        when(zoneLookup.listAll()).thenReturn(zones);

        assertEquals(zones, service.zones());
    }
}