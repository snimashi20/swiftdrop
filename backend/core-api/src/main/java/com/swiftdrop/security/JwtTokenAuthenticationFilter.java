package com.swiftdrop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Component
public class JwtTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;

    public JwtTokenAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = tokenProvider.parseClaims(token);
                Long userId = ((Number) claims.get("userId")).longValue();
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = parseAuthorities(claims.get("authorities"), role);
                AuthenticatedUser principal = new AuthenticatedUser(userId, email, role);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, null, authorities));
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(Object claim, String role) {
        Stream<?> values;
        if (claim == null) {
            values = Stream.empty();
        } else if (claim instanceof Collection<?> collection) {
            values = collection.stream();
        } else if (claim instanceof String authority) {
            values = Stream.of(authority);
        } else {
            log.warn("Ignoring malformed JWT authorities claim of type {}", claim.getClass().getName());
            values = Stream.empty();
        }

        List<SimpleGrantedAuthority> authorities = values
                .filter(value -> {
                    if (value == null || value.toString().isBlank()) {
                        log.warn("Ignoring malformed value in JWT authorities claim");
                        return false;
                    }
                    return true;
                })
                .map(Object::toString)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return authorities.isEmpty() && role != null && !role.isBlank()
                ? List.of(new SimpleGrantedAuthority(role))
                : authorities;
    }
}
