package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.CreateStaffRequestDto;
import com.swiftdrop.auth.dto.StatusToggleDto;
import com.swiftdrop.auth.dto.UpdateRoleDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import jakarta.validation.Valid;
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

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * GET /api/v1/admin/users
     * Retrieve all users, optionally filtered by role (e.g. ?role=COURIER)
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String role) {

        List<User> users;
        if (role != null && !role.isBlank()) {
            users = userRepository.findByRoleIgnoreCase(role.trim());
        } else {
            users = userRepository.findAll();
        }

        List<UserResponseDto> result = users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
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
                .build();
    }
}