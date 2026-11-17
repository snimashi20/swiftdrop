package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.AdminUserDetailsResponseDto;
import com.swiftdrop.auth.dto.CreateStaffRequestDto;
import com.swiftdrop.auth.dto.RejectUserRequestDto;
import com.swiftdrop.auth.dto.StatusToggleDto;
import com.swiftdrop.auth.dto.UpdateRoleDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.dispatch.service.ZoneLookup;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.repository.MerchantRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MerchantRepository merchantRepository;
    private final CourierProfileRepository courierProfileRepository;
    private final ZoneLookup zoneLookup;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null, null, null);
    }

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               MerchantRepository merchantRepository) {
        this(userRepository, passwordEncoder, merchantRepository, null, null);
    }

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               MerchantRepository merchantRepository,
                               CourierProfileRepository courierProfileRepository) {
        this(userRepository, passwordEncoder, merchantRepository, courierProfileRepository, null);
    }

    @Autowired
    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               @Autowired(required = false) MerchantRepository merchantRepository,
                               @Autowired(required = false) CourierProfileRepository courierProfileRepository,
                               @Autowired(required = false) ZoneLookup zoneLookup) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.merchantRepository = merchantRepository;
        this.courierProfileRepository = courierProfileRepository;
        this.zoneLookup = zoneLookup;
    }

    /**
     * GET /api/v1/admin/users/{id}
     * Retrieve full details of a single user, including any linked merchant or courier profiles.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailsResponseDto> getUserDetails(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        AdminUserDetailsResponseDto.AdminUserDetailsResponseDtoBuilder builder = AdminUserDetailsResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .provider(user.getProvider())
                .enabled(user.isEnabled())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt());

        if (merchantRepository != null) {
            merchantRepository.findByUserId(user.getId()).ifPresent(m -> {
                builder.merchantProfile(AdminUserDetailsResponseDto.MerchantDetailsDto.builder()
                        .id(m.getId())
                        .businessName(m.getBusinessName())
                        .registrationNumber(m.getRegistrationNumber())
                        .contactName(m.getContactName())
                        .contactEmail(m.getContactEmail())
                        .contactPhone(m.getContactPhone())
                        .status(m.getStatus() != null ? m.getStatus().name() : null)
                        .rejectionReason(m.getRejectionReason())
                        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                        .build());
            });
        }

        if (courierProfileRepository != null) {
            courierProfileRepository.findByUserId(user.getId()).ifPresent(c -> {
                String zoneName = (zoneLookup != null)
                        ? zoneLookup.districtOf(c.getZoneId()).map(d -> d + " Zone").orElse(null)
                        : null;
                builder.courierProfile(AdminUserDetailsResponseDto.CourierDetailsDto.builder()
                        .id(c.getId())
                        .vehicleType(c.getVehicleType() != null ? c.getVehicleType().name() : null)
                        .zoneId(c.getZoneId())
                        .zoneName(zoneName)
                        .active(c.isActive())
                        .nicNumber(c.getNicNumber())
                        .dateOfBirth(c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : null)
                        .residentialAddress(c.getResidentialAddress())
                        .drivingLicenseNumber(c.getDrivingLicenseNumber())
                        .drivingLicenseExpiry(c.getDrivingLicenseExpiry() != null ? c.getDrivingLicenseExpiry().toString() : null)
                        .vehiclePlateNumber(c.getVehiclePlateNumber())
                        .emergencyContactName(c.getEmergencyContactName())
                        .emergencyContactPhone(c.getEmergencyContactPhone())
                        .nicFrontPhotoUrl(c.getNicFrontPhotoUrl())
                        .nicBackPhotoUrl(c.getNicBackPhotoUrl())
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build());
            });
        }

        return ResponseEntity.ok(builder.build());
    }

    /**
     * GET /api/v1/admin/users
     * Retrieve all users, optionally filtered by role (e.g. ?role=COURIER) and status (e.g. ?status=PENDING_APPROVAL)
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        List<User> users;
        if (role != null && !role.isBlank()) {
            users = userRepository.findByRoleIgnoreCase(role.trim());
        } else {
            users = userRepository.findAll();
        }

        if (status != null && !status.isBlank()) {
            String targetStatus = status.trim();
            users = users.stream()
                    .filter(u -> targetStatus.equalsIgnoreCase(u.getStatus()))
                    .collect(Collectors.toList());
        }

        List<UserResponseDto> result = users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/v1/admin/users/{id}/approve
     * Approve a pending user account (e.g. Merchant or Courier)
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<UserResponseDto> approveUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setStatus("ACTIVE");
        user.setEnabled(true);
        User updated = userRepository.save(user);

        if (merchantRepository != null) {
            merchantRepository.findByUserId(user.getId()).ifPresent(m -> {
                m.setStatus(MerchantStatus.APPROVED);
                m.setRejectionReason(null);
                merchantRepository.save(m);
            });
        }

        if (courierProfileRepository != null) {
            courierProfileRepository.findByUserId(user.getId()).ifPresent(c -> {
                c.setActive(true);
                courierProfileRepository.save(c);
            });
        }

        return ResponseEntity.ok(mapToDto(updated));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/reject
     * Reject a pending user account with optional reason
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<UserResponseDto> rejectUser(
            @PathVariable Long id,
            @RequestBody(required = false) RejectUserRequestDto rejectDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setStatus("REJECTED");
        user.setEnabled(false);
        User updated = userRepository.save(user);

        if (merchantRepository != null) {
            merchantRepository.findByUserId(user.getId()).ifPresent(m -> {
                m.setStatus(MerchantStatus.REJECTED);
                if (rejectDto != null && rejectDto.getReason() != null && !rejectDto.getReason().isBlank()) {
                    m.setRejectionReason(rejectDto.getReason().trim());
                }
                merchantRepository.save(m);
            });
        }

        if (courierProfileRepository != null) {
            courierProfileRepository.findByUserId(user.getId()).ifPresent(c -> {
                c.setActive(false);
                courierProfileRepository.save(c);
            });
        }

        return ResponseEntity.ok(mapToDto(updated));
    }

    public ResponseEntity<UserResponseDto> rejectUser(Long id) {
        return rejectUser(id, (RejectUserRequestDto) null);
    }

    /**
     * PATCH /api/v1/admin/users/{id}/status
     * Enable or disable a user account
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusToggleDto statusDto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setEnabled(statusDto.isEnabled());
        if (statusDto.isEnabled()) {
            user.setStatus("ACTIVE");
        } else {
            user.setStatus("INACTIVE");
        }
        User updated = userRepository.save(user);

        return ResponseEntity.ok(mapToDto(updated));
    }

    /**
     * PUT /api/v1/admin/users/{id}/role
     * Update user role (e.g., CUSTOMER, COURIER, DISPATCHER, MERCHANT, ADMIN)
     */
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleDto roleDto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setRole(roleDto.getRole().trim().toUpperCase());
        User updated = userRepository.save(user);

        return ResponseEntity.ok(mapToDto(updated));
    }

    /**
     * POST /api/v1/admin/users/staff
     * Create Courier or Dispatcher staff account (restricted to COURIER or DISPATCHER)
     */
    @PostMapping("/staff")
    public ResponseEntity<UserResponseDto> createStaffAccount(@Valid @RequestBody CreateStaffRequestDto requestDto) {
        String role = requestDto.getRole() != null ? requestDto.getRole().trim().toUpperCase() : "";
        if (!"COURIER".equals(role) && !"DISPATCHER".equals(role)) {
            throw new BadRequestException("Staff role must be either COURIER or DISPATCHER");
        }

        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new ConflictException("User already exists with email: " + requestDto.getEmail());
        }

        User staffUser = new User();
        staffUser.setEmail(requestDto.getEmail());
        staffUser.setFullName(requestDto.getFullName());
        staffUser.setPhoneNumber(requestDto.getPhoneNumber());
        staffUser.setRole(role);
        staffUser.setPasswordHash(passwordEncoder.encode(requestDto.getPassword()));
        staffUser.setProvider("LOCAL");
        staffUser.setEnabled(true);
        staffUser.setStatus("ACTIVE");

        User saved = userRepository.save(staffUser);
        return ResponseEntity.ok(mapToDto(saved));
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .provider(user.getProvider())
                .enabled(user.isEnabled())
                .status(user.getStatus())
                .build();
    }
}