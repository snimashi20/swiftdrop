package com.swiftdrop.common.config;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.security.JwtService;
import com.swiftdrop.security.JwtTokenAuthenticationFilter;
import com.swiftdrop.auth.security.OAuth2AuthenticationSuccessHandler;
import com.swiftdrop.common.security.RestAccessDeniedHandler;
import com.swiftdrop.common.security.RestAuthenticationEntryPoint;
import com.swiftdrop.controller.AuthController;
import com.swiftdrop.dto.auth.AuthResponse;
import com.swiftdrop.merchant.api.MerchantAdminController;
import com.swiftdrop.merchant.api.MerchantController;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.security.ApiKeyAccessDeniedHandler;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationEntryPoint;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationFilter;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.merchant.service.ApiKeyService;
import com.swiftdrop.merchant.service.MerchantOrderService;
import com.swiftdrop.merchant.service.MerchantService;
import com.swiftdrop.security.JwtTokenProvider;
import com.swiftdrop.service.AuthService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MerchantAdminController.class, MerchantController.class, AuthController.class})
@AutoConfigureMockMvc(addFilters = true)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        MerchantAdminController.class,
        MerchantController.class,
        AuthController.class,
        JwtService.class,
        JwtTokenProvider.class,
        JwtTokenAuthenticationFilter.class,
        ApiKeyAuthenticationFilter.class,
        ApiKeyAuthenticationEntryPoint.class,
        ApiKeyAccessDeniedHandler.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=01234567890123456789012345678901",
        "spring.security.oauth2.client.registration.google.client-id=security-test-client",
        "spring.security.oauth2.client.registration.google.client-secret=security-test-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,email,profile",
        "spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}"
})
class SecurityConfigIntegrationTest {

    private static final String ADMIN_EMAIL = "security-admin@example.test";
    private static final String MERCHANT_EMAIL = "security-merchant@example.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MerchantService merchantService;

    @MockitoBean
    private MerchantOrderService merchantOrderService;

    @MockitoBean
    private CurrentMerchantProvider currentMerchantProvider;

    @MockitoBean
    private ApiKeyService apiKeyService;

    private String adminJwt;
    private String merchantJwt;

    @BeforeEach
    void setUp() {
        User admin = user(1L, ADMIN_EMAIL, "ADMIN");
        User merchant = user(2L, MERCHANT_EMAIL, "MERCHANT");
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail(MERCHANT_EMAIL)).thenReturn(Optional.of(merchant));

        adminJwt = jwtTokenProvider.generateToken(admin.getId(), admin.getEmail(), java.util.Set.of(admin.getRole()));
        merchantJwt = jwtTokenProvider.generateToken(
                merchant.getId(), merchant.getEmail(), java.util.Set.of(merchant.getRole()));

        when(merchantService.listForAdmin(any(), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(merchantService.register(any())).thenReturn(MerchantResponseDto.builder()
                .id(33L)
                .businessName("New Test Merchant")
                .registrationNumber("SECURITY-TEST-33")
                .contactName("Test User")
                .contactEmail("new-merchant@example.test")
                .contactPhone("0700000033")
                .status(MerchantStatus.PENDING)
                .build());
        when(merchantService.getMyProfile(any())).thenReturn(MerchantResponseDto.builder()
                .id(22L)
                .userId(2L)
                .businessName("Test Merchant")
                .registrationNumber("SECURITY-TEST-22")
                .contactName("Test User")
                .contactEmail(MERCHANT_EMAIL)
                .contactPhone("0700000022")
                .status(MerchantStatus.APPROVED)
                .build());
        when(authService.login(any())).thenReturn(new AuthResponse(
                "test-token", "Bearer", 1L, ADMIN_EMAIL, "Admin User", java.util.Set.of("ADMIN")));
    }

    @Test
    void adminJwtCanAccessAdminMerchantList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants").header("Authorization", bearer(adminJwt)))
                .andExpect(status().isOk());
    }

    @Test
    void merchantJwtGetsJsonForbiddenFromAdminMerchantList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants").header("Authorization", bearer(merchantJwt)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Access is denied"))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/merchants"));
    }

    @Test
    void missingJwtGetsJsonUnauthorizedFromAdminMerchantList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }

    @Test
    void invalidJwtGetsJsonUnauthorizedFromAdminMerchantList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants").header("Authorization", bearer("not-a-valid-jwt")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void merchantJwtCanAccessMerchantProfile() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me").header("Authorization", bearer(merchantJwt)))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpointRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.test\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void merchantRegistrationRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new-merchant@example.test\",\"password\":\"test-password\","
                                + "\"businessName\":\"New Test Merchant\",\"registrationNumber\":\"SECURITY-TEST-33\","
                                + "\"contactName\":\"Test User\",\"contactPhone\":\"0700000033\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void existingPublicTrackingRouteDoesNotRequireJwt() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/security-test-tracking"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    void browserOauthInitiationStillRedirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("accounts.google.com")));
    }

    @Test
    void oauthCallbackIsAssignedToBrowserChainAndNotJwtFilter() {
        List<Filter> filters = springSecurityFilterChain.getFilters("/login/oauth2/code/google");

        assertThat(filters).anyMatch(OAuth2LoginAuthenticationFilter.class::isInstance);
        assertThat(filters).noneMatch(JwtTokenAuthenticationFilter.class::isInstance);
    }

    @Test
    void missingAndInvalidApiKeysRemainJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/merchant-api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));

        when(apiKeyService.hashKey("invalid-test-key")).thenReturn("hashed-test-key");
        when(apiKeyService.findActiveKeyByHash("hashed-test-key")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/merchant-api/orders").header("X-API-Key", "invalid-test-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static User user(Long id, String email, String role) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName(role + " User")
                .role(role)
                .enabled(true)
                .build();
    }
}
