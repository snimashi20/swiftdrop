package com.swiftdrop.auth.security;

import com.swiftdrop.auth.domain.Role;
import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendBaseUrl;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        Boolean emailVerified = oAuth2User.getAttribute("email_verified");
        if (Boolean.FALSE.equals(emailVerified)) {
            log.warn("OAuth2 sign-in rejected: Google account email is not verified");
            getRedirectStrategy().sendRedirect(request, response, loginErrorUrl("email_not_verified"));
            return;
        }

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleSub = oAuth2User.getAttribute("sub");

        if (email == null || email.isBlank()) {
            log.warn("OAuth2 sign-in rejected: No email found in OAuth2 attributes");
            getRedirectStrategy().sendRedirect(request, response, loginErrorUrl("missing_email"));
            return;
        }

        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            user.setProvider("GOOGLE");
            user.setProviderId(googleSub);
            if (name != null && !name.isBlank() && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(name);
            }

            // If the existing user had PENDING_ROLE, resolve role now
            if ("PENDING_ROLE".equalsIgnoreCase(user.getRole())) {
                String requestedRole = extractAndClearRole(request, response);
                boolean requiresApproval = Role.requiresApproval(requestedRole);
                user.setRole(requestedRole);
                user.setEnabled(!requiresApproval);
                user.setStatus(requiresApproval ? "PENDING_APPROVAL" : "ACTIVE");
                user = userRepository.save(user);

                if (requiresApproval) {
                    getRedirectStrategy().sendRedirect(request, response, pendingApprovalUrl());
                    return;
                }

                String jwtToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus());
                String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                        .path("/oauth/callback")
                        .queryParam("token", jwtToken)
                        .queryParam("newUser", "true")
                        .build().toUriString();
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }

            user = userRepository.save(user);

            // Handle pending approval
            if ("PENDING_APPROVAL".equalsIgnoreCase(user.getStatus()) || (!user.isEnabled() && "PENDING_APPROVAL".equalsIgnoreCase(user.getStatus()))) {
                getRedirectStrategy().sendRedirect(request, response, pendingApprovalUrl());
                return;
            }

            // Handle disabled / inactive / rejected accounts
            if (!user.isEnabled() || "INACTIVE".equalsIgnoreCase(user.getStatus()) || "REJECTED".equalsIgnoreCase(user.getStatus())) {
                getRedirectStrategy().sendRedirect(request, response, loginErrorUrl("account_disabled"));
                return;
            }

            // Existing active user login
            String jwtToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus());
            String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                    .path("/oauth/callback")
                    .queryParam("token", jwtToken)
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // New Google user registration flow
        String requestedRole = extractAndClearRole(request, response);
        boolean requiresApproval = Role.requiresApproval(requestedRole);

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(name != null && !name.isBlank() ? name : email);
        newUser.setRole(requestedRole);
        newUser.setProvider("GOOGLE");
        newUser.setProviderId(googleSub);
        newUser.setEnabled(!requiresApproval);
        newUser.setStatus(requiresApproval ? "PENDING_APPROVAL" : "ACTIVE");
        User savedUser = userRepository.save(newUser);

        if (requiresApproval) {
            log.info("New OAuth2 user created with role {} pending admin approval: {}", requestedRole, email);
            getRedirectStrategy().sendRedirect(request, response, pendingApprovalUrl());
            return;
        }

        log.info("New OAuth2 customer registered: {}", email);
        String jwtToken = jwtService.generateToken(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole(), savedUser.getStatus());
        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/oauth/callback")
                .queryParam("token", jwtToken)
                .queryParam("newUser", "true")
                .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String extractAndClearRole(HttpServletRequest request, HttpServletResponse response) {
        String role = null;

        // 1. Direct query parameter fallback
        if (request != null) {
            role = request.getParameter(CustomOAuth2AuthorizationRequestResolver.ROLE_PARAM);

            // 2. Session attribute
            if (role == null || role.isBlank()) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Object sessionRole = session.getAttribute(CustomOAuth2AuthorizationRequestResolver.ROLE_SESSION_ATTRIBUTE);
                    if (sessionRole != null) {
                        role = sessionRole.toString();
                        session.removeAttribute(CustomOAuth2AuthorizationRequestResolver.ROLE_SESSION_ATTRIBUTE);
                    }
                }
            }

            // 3. Short-lived cookie
            if (role == null || role.isBlank()) {
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie c : cookies) {
                        if (CustomOAuth2AuthorizationRequestResolver.ROLE_COOKIE_NAME.equals(c.getName())) {
                            role = c.getValue();
                            break;
                        }
                    }
                }
            }
        }

        // Clear cookie if response is available
        if (response != null) {
            Cookie clearCookie = new Cookie(CustomOAuth2AuthorizationRequestResolver.ROLE_COOKIE_NAME, "");
            clearCookie.setPath("/");
            clearCookie.setMaxAge(0);
            clearCookie.setHttpOnly(true);
            response.addCookie(clearCookie);
        }

        // Normalize or fallback to ROLE_CUSTOMER (disallowing ADMIN/DISPATCHER)
        return Role.normalizeOrDefault(role, Role.CUSTOMER);
    }

    private String pendingApprovalUrl() {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/oauth/callback")
                .queryParam("error", "PendingAdminApproval")
                .build().toUriString();
    }

    private String loginErrorUrl(String error) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/login")
                .queryParam("error", error)
                .build().toUriString();
    }
}
