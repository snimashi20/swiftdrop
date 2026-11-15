package com.swiftdrop.auth.security;

import com.swiftdrop.auth.domain.Role;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String ROLE_PARAM = "role";
    public static final String ROLE_COOKIE_NAME = "swiftdrop_oauth2_role";
    public static final String ROLE_SESSION_ATTRIBUTE = "SWIFTDROP_OAUTH2_ROLE";
    public static final int COOKIE_MAX_AGE_SECONDS = 300; // 5 minutes

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        if (authorizationRequest != null) {
            captureRoleParameter(request);
        }
        return authorizationRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        if (authorizationRequest != null) {
            captureRoleParameter(request);
        }
        return authorizationRequest;
    }

    private void captureRoleParameter(HttpServletRequest request) {
        String roleParam = request.getParameter(ROLE_PARAM);
        if (roleParam != null && !roleParam.isBlank()) {
            // Whitelist against self-registerable roles (CUSTOMER, MERCHANT, COURIER).
            // Reject / ignore ADMIN or DISPATCHER.
            String validatedRole = Role.normalizeOrDefault(roleParam, null);
            if (validatedRole != null) {
                log.info("Captured OAuth2 requested role: {}", validatedRole);

                // 1. Store in HTTP session
                HttpSession session = request.getSession(true);
                session.setAttribute(ROLE_SESSION_ATTRIBUTE, validatedRole);

                // 2. Store in short-lived HTTP-only cookie
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null && attrs.getResponse() != null) {
                    HttpServletResponse response = attrs.getResponse();
                    Cookie cookie = new Cookie(ROLE_COOKIE_NAME, validatedRole);
                    cookie.setPath("/");
                    cookie.setHttpOnly(true);
                    cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
                    response.addCookie(cookie);
                }
            } else {
                log.warn("Ignored unauthorized OAuth2 registration role parameter: {}", roleParam);
            }
        }
    }
}

