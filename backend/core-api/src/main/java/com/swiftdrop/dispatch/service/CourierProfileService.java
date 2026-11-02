package com.swiftdrop.dispatch.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.dto.CourierProfileCreateRequest;
import com.swiftdrop.dispatch.dto.CourierProfileMapper;
import com.swiftdrop.dispatch.dto.CourierProfileResponse;
import com.swiftdrop.dispatch.dto.CourierProfileUpdateRequest;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourierProfileService {

    private final CourierProfileRepository repository;
    private final CourierProfileMapper mapper;
    private final UserRepository userRepository;
    private final ZoneLookup zoneLookup;

    @Transactional
    public CourierProfileResponse create(CourierProfileCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!isCourier(user)) {
            throw new BadRequestException("User does not have the COURIER role");
        }
        if (!zoneLookup.exists(request.zoneId())) {
            throw new NotFoundException("Zone not found");
        }
        if (repository.existsByUserId(request.userId())) {
            throw new ConflictException("A courier profile already exists for this user");
        }
        CourierProfile profile = CourierProfile.builder()
                .userId(request.userId())
                .vehicleType(request.vehicleType())
                .zoneId(request.zoneId())
                .active(request.active() == null || request.active())
                .build();
        CourierProfile saved = repository.save(profile);
        log.info("Courier profile created id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CourierProfileResponse get(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CourierProfileResponse> list(Long zoneId, Boolean active, Pageable pageable) {
        Page<CourierProfile> page;
        if (zoneId != null && active != null) {
            page = repository.findByZoneIdAndActive(zoneId, active, pageable);
        } else if (zoneId != null) {
            page = repository.findByZoneId(zoneId, pageable);
        } else if (active != null) {
            page = repository.findByActive(active, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(mapper::toResponse);
    }

    @Transactional
    public CourierProfileResponse update(Long id, CourierProfileUpdateRequest request) {
        CourierProfile profile = findOrThrow(id);
        if (request.vehicleType() != null) {
            profile.setVehicleType(request.vehicleType());
        }
        if (request.zoneId() != null) {
            if (!zoneLookup.exists(request.zoneId())) {
                throw new NotFoundException("Zone not found");
            }
            profile.setZoneId(request.zoneId());
        }
        if (request.active() != null) {
            profile.setActive(request.active());
        }
        CourierProfile saved = repository.save(profile);
        log.info("Courier profile updated id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    /** Package-private so tests can replace it (the role type belongs to the auth module). */
    boolean isCourier(User user) {
        return "COURIER".equals(String.valueOf(user.getRole()));
    }

    private CourierProfile findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Courier profile not found"));
    }
}