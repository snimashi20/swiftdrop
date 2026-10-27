package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.CreateStaffRequestDto;
import com.swiftdrop.auth.dto.StatusToggleDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
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
     * System එකේ සියලුම Users ලා ලබා ගැනීම
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * PATCH /api/v1/admin/users/{id}/status
     * User කෙනෙකුගේ Account Status එක Enable/Disable කිරීම
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDto> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody StatusToggleDto statusDto) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        user.setEnabled(statusDto.isEnabled());
        User updated = userRepository.save(user);

        return ResponseEntity.ok(mapToDto(updated));
    }

    /**
     * POST /api/v1/admin/users/staff
     * Admin විසින් අලුතින් Courier / Dispatcher Accounts සෑදීම
     */
    @PostMapping("/staff")
    public ResponseEntity<UserResponseDto> createStaffAccount(@Valid @RequestBody CreateStaffRequestDto requestDto) {
        if (userRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new ConflictException("User already exists with email: " + requestDto.getEmail());
        }

        User staffUser = new User();
        staffUser.setEmail(requestDto.getEmail());
        staffUser.setFullName(requestDto.getFullName());
        staffUser.setPhoneNumber(requestDto.getPhoneNumber());
        staffUser.setRole(requestDto.getRole().toUpperCase());
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