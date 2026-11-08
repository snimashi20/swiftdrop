package com.swiftdrop.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.dto.auth.AuthResponse;
import com.swiftdrop.dto.auth.LoginRequest;
import com.swiftdrop.dto.auth.RegisterRequest;
import com.swiftdrop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";
        private static final Set<String> SELF_REGISTRATION_ROLES = Set.of(
            "ROLE_CUSTOMER",
            "ROLE_MERCHANT",
            "ROLE_COURIER");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .role(normalizeRole(request.role()))
                .build();
        User saved = userRepository.save(user);
        return response(saved, tokenProvider.generateToken(saved.getId(), saved.getEmail(), Set.of(saved.getRole())));
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = (User) authentication.getPrincipal();
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), Set.of(user.getRole()));
        return response(user, token);
    }

    private AuthResponse response(User user, String token) {
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getName(), Set.of(user.getRole()));
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return DEFAULT_ROLE;
        }
        String normalized = role.trim().toUpperCase();
        String normalizedRole = normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
        if (!SELF_REGISTRATION_ROLES.contains(normalizedRole)) {
            throw new IllegalArgumentException("Unauthorized self-registration role: " + role);
        }
        return normalizedRole;
    }
}
