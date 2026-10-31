package com.swiftdrop.auth.api;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.dto.UpdateProfileRequestDto;
import com.swiftdrop.auth.dto.UserResponseDto;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUserProfile() {
        User currentUser = getAuthenticatedUser();
        return ResponseEntity.ok(mapToResponseDto(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateProfile(@Valid @RequestBody UpdateProfileRequestDto requestDto) {
        User currentUser = getAuthenticatedUser();

        currentUser.setFullName(requestDto.getFullName());
        currentUser.setPhoneNumber(requestDto.getPhoneNumber());

        User updatedUser = userRepository.save(currentUser);
        return ResponseEntity.ok(mapToResponseDto(updatedUser));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
                .build();
    }
}