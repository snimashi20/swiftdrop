package com.swiftdrop.auth.security;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

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

        User user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    existingUser.setProvider("GOOGLE");
                    existingUser.setProviderId(googleSub);
                    if (name != null && !name.isBlank() && (existingUser.getFullName() == null || existingUser.getFullName().isBlank())) {
                        existingUser.setFullName(name);
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setFullName(name != null && !name.isBlank() ? name : email);
                    newUser.setRole("CUSTOMER");
                    newUser.setProvider("GOOGLE");
                    newUser.setProviderId(googleSub);
                    newUser.setEnabled(true);
                    return userRepository.save(newUser);
                });

        if (!user.isEnabled()) {
            getRedirectStrategy().sendRedirect(request, response, loginErrorUrl("account_disabled"));
            return;
        }

        String jwtToken = jwtService.generateToken(user.getId(), user.getEmail(), user.getFullName(), user.getRole());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/oauth/callback")
                .queryParam("token", jwtToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String loginErrorUrl(String error) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/login")
                .queryParam("error", error)
                .build().toUriString();
    }
}