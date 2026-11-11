package com.swiftdrop.service;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.dto.auth.AuthResponse;
import com.swiftdrop.dto.auth.RegisterRequest;
import com.swiftdrop.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersAllowedRole() {
        RegisterRequest request = new RegisterRequest("Merchant", "merchant@example.com", "password", "merchant");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(tokenProvider.generateToken(7L, "merchant@example.com", java.util.Set.of("ROLE_MERCHANT"), "PENDING_APPROVAL"))
                .thenReturn("token");

        AuthResponse response = authService.register(request);

        assertThat(response.roles()).containsExactly("ROLE_MERCHANT");
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.status()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void registersCustomerWithActiveStatus() {
        RegisterRequest request = new RegisterRequest("Customer", "cust@example.com", "password", "customer");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(8L);
            return user;
        });
        when(tokenProvider.generateToken(8L, "cust@example.com", java.util.Set.of("ROLE_CUSTOMER"), "ACTIVE"))
                .thenReturn("token");

        AuthResponse response = authService.register(request);

        assertThat(response.roles()).containsExactly("ROLE_CUSTOMER");
        assertThat(response.token()).isEqualTo("token");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void rejectsPrivilegedRoleDuringSelfRegistration() {
        RegisterRequest request = new RegisterRequest("Admin", "admin@example.com", "password", "ADMIN");
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unauthorized self-registration role");
        verify(userRepository, never()).save(any(User.class));
    }
}
