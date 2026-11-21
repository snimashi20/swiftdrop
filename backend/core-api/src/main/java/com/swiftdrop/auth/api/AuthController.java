package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.ChangePasswordRequestDto;
import com.swiftdrop.auth.dto.UpdateProfileRequestDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUserProfile(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(mapToResponseDto(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto requestDto,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        currentUser.setFullName(requestDto.getFullName());
        currentUser.setPhoneNumber(requestDto.getPhoneNumber());

        User updatedUser = userRepository.save(currentUser);
        return ResponseEntity.ok(mapToResponseDto(updatedUser));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto requestDto,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);

        if (!"LOCAL".equals(currentUser.getProvider())) {
            return ResponseEntity.badRequest().build();
        }
        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), currentUser.getPasswordHash())) {
            return ResponseEntity.status(403).build();
        }

        currentUser.setPasswordHash(passwordEncoder.encode(requestDto.getNewPassword()));
        userRepository.save(currentUser);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User profile not found for email: " + email));
    }

    private UserResponseDto mapToResponseDto(User user) {
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