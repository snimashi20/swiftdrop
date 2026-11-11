package com.swiftdrop.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.dto.auth.AuthResponse;
import com.swiftdrop.dto.auth.LoginRequest;
import com.swiftdrop.dto.auth.RegisterRequest;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.repository.MerchantRepository;
import com.swiftdrop.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
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
    private final MerchantRepository merchantRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this(userRepository, passwordEncoder, tokenProvider, authenticationManager, null);
    }

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       @Autowired(required = false) MerchantRepository merchantRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        String normalizedRole = normalizeRole(request.role());
        boolean isPending = "ROLE_MERCHANT".equalsIgnoreCase(normalizedRole) || "ROLE_COURIER".equalsIgnoreCase(normalizedRole);
        String initialStatus = isPending ? "PENDING_APPROVAL" : "ACTIVE";

        User user = User.builder()
                .fullName(request.name())
                .email(request.email().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(normalizedRole)
                .status(initialStatus)
                .enabled(!isPending)
                .build();
        User saved = userRepository.save(user);

        if ("ROLE_MERCHANT".equalsIgnoreCase(normalizedRole)) {
            createMerchantProfileIfMissing(saved);
        }

        String token = tokenProvider.generateToken(saved.getId(), saved.getEmail(), Set.of(saved.getRole()), saved.getStatus());
        return response(saved, token);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getPasswordHash() != null && !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if ("PENDING_APPROVAL".equalsIgnoreCase(user.getStatus())) {
            String token = tokenProvider.generateToken(user.getId(), user.getEmail(), Set.of(user.getRole()), user.getStatus());
            return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getFullName(), Set.of(user.getRole()), "PENDING_APPROVAL");
        }

        if (!user.isEnabled() || "REJECTED".equalsIgnoreCase(user.getStatus()) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new DisabledException("Your account has been deactivated or rejected");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User authUser = (User) authentication.getPrincipal();
        String token = tokenProvider.generateToken(authUser.getId(), authUser.getEmail(), Set.of(authUser.getRole()), authUser.getStatus());
        return response(authUser, token);
    }

    @Transactional
    public AuthResponse selectRole(Authentication authentication, String chosenRole) {
        if (authentication == null || authentication.getName() == null) {
            throw new BadCredentialsException("Authentication required to select a role");
        }

        String email = authentication.getName().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("User not found for email: " + email));

        String normalizedRole = normalizeRole(chosenRole);
        boolean isPending = "ROLE_MERCHANT".equalsIgnoreCase(normalizedRole) || "ROLE_COURIER".equalsIgnoreCase(normalizedRole);
        String nextStatus = isPending ? "PENDING_APPROVAL" : "ACTIVE";

        user.setRole(normalizedRole);
        user.setStatus(nextStatus);
        user.setEnabled(!isPending);
        User saved = userRepository.save(user);

        if ("ROLE_MERCHANT".equalsIgnoreCase(normalizedRole)) {
            createMerchantProfileIfMissing(saved);
        }

        String token = tokenProvider.generateToken(saved.getId(), saved.getEmail(), Set.of(saved.getRole()), saved.getStatus());
        return response(saved, token);
    }

    private void createMerchantProfileIfMissing(User user) {
        if (merchantRepository == null) return;
        if (merchantRepository.findByUserId(user.getId()).isEmpty()) {
            Merchant merchant = Merchant.builder()
                    .userId(user.getId())
                    .businessName(user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() + "'s Store" : "Store")
                    .registrationNumber("REG-" + user.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .contactName(user.getFullName() != null ? user.getFullName() : user.getEmail())
                    .contactEmail(user.getEmail())
                    .contactPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "0770000000")
                    .status(MerchantStatus.PENDING)
                    .build();
            merchantRepository.save(merchant);
        }
    }

    private AuthResponse response(User user, String token) {
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getFullName(), Set.of(user.getRole()), user.getStatus());
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
