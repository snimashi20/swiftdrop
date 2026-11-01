package com.swiftdrop.merchant.security;

import com.swiftdrop.merchant.domain.ApiKey;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyService apiKeyService;
    private final ApiKeyAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiKeyAccessDeniedHandler accessDeniedHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/merchant-api");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String rawKey = request.getHeader(API_KEY_HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or missing API key")
            );
            return;
        }

        String keyHash;
        try {
            keyHash = apiKeyService.hashKey(rawKey.trim());
        } catch (Exception e) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or missing API key")
            );
            return;
        }

        Optional<ApiKey> apiKeyOpt = apiKeyService.findActiveKeyByHash(keyHash);
        if (apiKeyOpt.isEmpty()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or missing API key")
            );
            return;
        }

        ApiKey apiKey = apiKeyOpt.get();
        Merchant merchant = apiKey.getMerchant();
        if (merchant == null || merchant.getStatus() != MerchantStatus.APPROVED) {
            accessDeniedHandler.handle(
                    request,
                    response,
                    new AccessDeniedException("Merchant is not approved")
            );
            return;
        }

        apiKeyService.recordKeyUsage(apiKey.getId());

        MerchantPrincipal principal = new MerchantPrincipal(merchant.getId());
        ApiKeyAuthenticationToken authentication = new ApiKeyAuthenticationToken(
                principal,
                List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
