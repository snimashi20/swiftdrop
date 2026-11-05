package com.swiftdrop.notification.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Turns the Bearer token core-api issued into an {@link AuthenticatedUser} principal.
 *
 * <p>A missing or bad token leaves the context empty; SecurityConfig then answers 401, so this
 * filter never writes to the response itself.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDirectory users;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtService.extractClaims(token);
                String email = claims.get("email", String.class);
                if (email == null) {
                    email = claims.getSubject();
                }

                users.findByEmail(email)
                        .filter(UserDirectory.Account::enabled)
                        .ifPresent(this::authenticate);
            } catch (Exception e) {
                // Never log the token itself.
                log.debug("Rejected a Bearer token: {}", e.getClass().getSimpleName());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(UserDirectory.Account account) {
        AuthenticatedUser principal = new AuthenticatedUser(account.id(), account.email(), account.role());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + account.role())))
        );
    }
}
