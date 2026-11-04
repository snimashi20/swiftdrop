package com.swiftdrop.dispatch.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.dto.CourierCandidateResponse;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Small lookups that fill the dropdowns of the dispatcher screens. */
@Service
@RequiredArgsConstructor
public class DispatchLookupService {

    private final UserRepository userRepository;
    private final CourierProfileRepository courierRepository;
    private final ZoneLookup zoneLookup;

    /** Enabled users with the COURIER role who have no courier profile yet, by name. */
    @Transactional(readOnly = true)
    public List<CourierCandidateResponse> courierCandidates() {
        Set<Long> withProfile = courierRepository.findAll().stream()
                .map(CourierProfile::getUserId)
                .collect(Collectors.toSet());
        return userRepository.findByRoleIgnoreCase("COURIER").stream()
                .filter(User::isEnabled)
                .filter(user -> !withProfile.contains(user.getId()))
                .map(user -> new CourierCandidateResponse(user.getId(), user.getFullName()))
                .sorted(Comparator.comparing(
                        (CourierCandidateResponse c) -> c.fullName() == null ? "" : c.fullName().toLowerCase()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ZoneLookup.ZoneOption> zones() {
        return zoneLookup.listAll();
    }
}