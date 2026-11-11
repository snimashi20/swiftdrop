package com.swiftdrop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenAuthenticationFilterTest {

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticatedUserForValidBearerToken() throws Exception {
        JwtTokenProvider provider = new JwtTokenProvider("01234567890123456789012345678901", 60_000);
        String token = provider.generateToken(12L, "courier@example.com", java.util.Set.of("ROLE_COURIER"));
        JwtTokenAuthenticationFilter filter = new JwtTokenAuthenticationFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer " + token);

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthenticatedUser(12L, "courier@example.com", "ROLE_COURIER"));
        assertThat(authentication.getName()).isEqualTo("courier@example.com");
        assertThat(authentication.getAuthorities()).extracting(Object::toString).contains("ROLE_COURIER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresMissingBearerHeader() throws Exception {
        JwtTokenAuthenticationFilter filter = new JwtTokenAuthenticationFilter(
                new JwtTokenProvider("01234567890123456789012345678901", 60_000));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void ignoresInvalidToken() throws Exception {
        JwtTokenAuthenticationFilter filter = new JwtTokenAuthenticationFilter(
                new JwtTokenProvider("01234567890123456789012345678901", 60_000));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer invalid");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void acceptsStringAuthorityClaim() throws Exception {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("userId")).thenReturn(12L);
        when(claims.get("email", String.class)).thenReturn("courier@example.com");
        when(claims.get("role", String.class)).thenReturn("ROLE_COURIER");
        when(claims.get("authorities")).thenReturn("ROLE_COURIER");
        JwtTokenProvider provider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        when(provider.parseClaims("token")).thenReturn(claims);
        JwtTokenAuthenticationFilter filter = new JwtTokenAuthenticationFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_COURIER");
    }

    @Test
    void fallsBackToPrimaryRoleForMalformedAuthorityClaim() throws Exception {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("userId")).thenReturn(12L);
        when(claims.get("email", String.class)).thenReturn("courier@example.com");
        when(claims.get("role", String.class)).thenReturn("ROLE_COURIER");
        when(claims.get("authorities")).thenReturn(42);
        JwtTokenProvider provider = org.mockito.Mockito.mock(JwtTokenProvider.class);
        when(provider.parseClaims("token")).thenReturn(claims);
        JwtTokenAuthenticationFilter filter = new JwtTokenAuthenticationFilter(provider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_COURIER");
    }
}
