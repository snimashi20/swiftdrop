package com.swiftdrop.auth.security;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OAuth2AuthenticationSuccessHandler successHandler;
    private final String frontendBaseUrl = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(userRepository, jwtService, frontendBaseUrl);
        successHandler.setRedirectStrategy(redirectStrategy);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
    }

    @Test
    @DisplayName("New Google user is auto-provisioned with CUSTOMER role and redirected with JWT")
    void onAuthenticationSuccess_newUser_provisionsCustomerAccountAndRedirectsWithJwt() throws Exception {
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("email")).thenReturn("newcustomer@gmail.com");
        when(oAuth2User.getAttribute("name")).thenReturn("New Customer");
        when(oAuth2User.getAttribute("sub")).thenReturn("google-sub-1001");

        when(userRepository.findByEmail("newcustomer@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(101L);
            return u;
        });

        when(jwtService.generateToken(101L, "newcustomer@gmail.com", "New Customer", "PENDING_ROLE", "PENDING_ROLE"))
                .thenReturn("mocked.jwt.token");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("newcustomer@gmail.com", savedUser.getEmail());
        assertEquals("New Customer", savedUser.getFullName());
        assertEquals("PENDING_ROLE", savedUser.getRole());
        assertEquals("GOOGLE", savedUser.getProvider());
        assertEquals("google-sub-1001", savedUser.getProviderId());
        assertTrue(savedUser.isEnabled());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectCaptor.capture());
        assertEquals("http://localhost:5173/oauth/callback?token=mocked.jwt.token&newUser=true", redirectCaptor.getValue());
    }

    @Test
    @DisplayName("Existing user account is linked with Google provider preserving existing role")
    void onAuthenticationSuccess_existingUser_linksGoogleProviderAndPreservesRole() throws Exception {
        User existingUser = new User();
        existingUser.setId(202L);
        existingUser.setEmail("courier@swiftdrop.lk");
        existingUser.setFullName("Existing Courier");
        existingUser.setRole("COURIER");
        existingUser.setProvider("LOCAL");
        existingUser.setEnabled(true);

        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("email")).thenReturn("courier@swiftdrop.lk");
        when(oAuth2User.getAttribute("name")).thenReturn("Google Courier Name");
        when(oAuth2User.getAttribute("sub")).thenReturn("google-sub-2002");

        when(userRepository.findByEmail("courier@swiftdrop.lk")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        when(jwtService.generateToken(202L, "courier@swiftdrop.lk", "Existing Courier", "COURIER"))
                .thenReturn("mocked.courier.jwt");

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals("GOOGLE", existingUser.getProvider());
        assertEquals("google-sub-2002", existingUser.getProviderId());
        assertEquals("COURIER", existingUser.getRole()); // role preserved

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectCaptor.capture());
        assertEquals("http://localhost:5173/oauth/callback?token=mocked.courier.jwt", redirectCaptor.getValue());
    }

    @Test
    @DisplayName("Unverified Google email is rejected and redirected with error")
    void onAuthenticationSuccess_unverifiedEmail_redirectsToLoginWithError() throws Exception {
        when(oAuth2User.getAttribute("email_verified")).thenReturn(false);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any(), any(), any());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectCaptor.capture());
        assertEquals("http://localhost:5173/login?error=email_not_verified", redirectCaptor.getValue());
    }

    @Test
    @DisplayName("Missing email attribute in OAuth2 attributes is rejected")
    void onAuthenticationSuccess_missingEmail_redirectsToLoginWithError() throws Exception {
        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("email")).thenReturn(null);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any(), any(), any());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectCaptor.capture());
        assertEquals("http://localhost:5173/login?error=missing_email", redirectCaptor.getValue());
    }

    @Test
    @DisplayName("Disabled user account is rejected and redirected with account_disabled error")
    void onAuthenticationSuccess_disabledAccount_redirectsToLoginWithError() throws Exception {
        User disabledUser = new User();
        disabledUser.setId(303L);
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setFullName("Disabled User");
        disabledUser.setRole("CUSTOMER");
        disabledUser.setEnabled(false);

        when(oAuth2User.getAttribute("email_verified")).thenReturn(true);
        when(oAuth2User.getAttribute("email")).thenReturn("disabled@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Disabled User");
        when(oAuth2User.getAttribute("sub")).thenReturn("google-sub-3003");

        when(userRepository.findByEmail("disabled@example.com")).thenReturn(Optional.of(disabledUser));
        when(userRepository.save(disabledUser)).thenReturn(disabledUser);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(jwtService, never()).generateToken(any(), any(), any(), any());

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), redirectCaptor.capture());
        assertEquals("http://localhost:5173/login?error=account_disabled", redirectCaptor.getValue());
    }
}
