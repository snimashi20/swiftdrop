package com.swiftdrop.merchant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swiftdrop.merchant.domain.ApiKey;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterMockMvcTest {

    @RestController
    @RequestMapping("/api/v1/merchant-api/test")
    static class TestMerchantApiController {
        @GetMapping
        public ResponseEntity<Map<String, Object>> testEndpoint(
                @AuthenticationPrincipal MerchantPrincipal principal
        ) {
            Long merchantId = principal != null ? principal.merchantId() : null;
            if (merchantId == null && SecurityContextHolder.getContext().getAuthentication() != null) {
                Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (p instanceof MerchantPrincipal mp) {
                    merchantId = mp.merchantId();
                }
            }
            Map<String, Object> body = new HashMap<>();
            body.put("status", "OK");
            body.put("merchantId", merchantId);
            return ResponseEntity.ok(body);
        }
    }

    @Mock
    private ApiKeyService apiKeyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        ApiKeyAuthenticationEntryPoint authenticationEntryPoint = new ApiKeyAuthenticationEntryPoint(objectMapper);
        ApiKeyAccessDeniedHandler accessDeniedHandler = new ApiKeyAccessDeniedHandler(objectMapper);
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter = new ApiKeyAuthenticationFilter(
                apiKeyService,
                authenticationEntryPoint,
                accessDeniedHandler
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestMerchantApiController())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .addFilters(apiKeyAuthenticationFilter)
                .build();
    }

    @Test
    @DisplayName("Valid key returns 200 OK and sets MerchantPrincipal")
    void validKey_returns200Ok() throws Exception {
        String testKey = "sdk_valid_dummy_key_12345678901234567890";
        String testHash = "hash123";

        Merchant merchant = Merchant.builder()
                .id(42L)
                .businessName("Apex Logistics")
                .status(MerchantStatus.APPROVED)
                .build();

        ApiKey apiKey = ApiKey.builder()
                .id(100L)
                .merchant(merchant)
                .keyHash(testHash)
                .active(true)
                .build();

        when(apiKeyService.hashKey(testKey)).thenReturn(testHash);
        when(apiKeyService.findActiveKeyByHash(testHash)).thenReturn(Optional.of(apiKey));

        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("X-API-Key", testKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.merchantId").value(42));

        verify(apiKeyService).recordKeyUsage(100L);
    }

    @Test
    @DisplayName("Missing API key returns 401 with INVALID_API_KEY code")
    void missingKey_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchant-api/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("Invalid or missing API key"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }

    @Test
    @DisplayName("Wrong / unknown API key returns 401 with INVALID_API_KEY code")
    void wrongKey_returns401() throws Exception {
        String testKey = "sdk_wrong_dummy_key_12345678901234567890";
        String testHash = "wrongHash";

        when(apiKeyService.hashKey(testKey)).thenReturn(testHash);
        when(apiKeyService.findActiveKeyByHash(testHash)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("X-API-Key", testKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("Invalid or missing API key"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }

    @Test
    @DisplayName("Revoked API key returns 401 with same INVALID_API_KEY code and message")
    void revokedKey_returns401() throws Exception {
        String testKey = "sdk_revoked_dummy_key_1234567890123456789";
        String testHash = "revokedHash";

        // findActiveKeyByHash returns empty for revoked keys because active is false
        when(apiKeyService.hashKey(testKey)).thenReturn(testHash);
        when(apiKeyService.findActiveKeyByHash(testHash)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("X-API-Key", testKey))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("Invalid or missing API key"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }

    @Test
    @DisplayName("Pending merchant returns 403 with MERCHANT_NOT_APPROVED code")
    void pendingMerchant_returns403() throws Exception {
        String testKey = "sdk_pending_dummy_key_1234567890123456789";
        String testHash = "pendingHash";

        Merchant pendingMerchant = Merchant.builder()
                .id(55L)
                .businessName("Pending Shop")
                .status(MerchantStatus.PENDING)
                .build();

        ApiKey apiKey = ApiKey.builder()
                .id(200L)
                .merchant(pendingMerchant)
                .keyHash(testHash)
                .active(true)
                .build();

        when(apiKeyService.hashKey(testKey)).thenReturn(testHash);
        when(apiKeyService.findActiveKeyByHash(testHash)).thenReturn(Optional.of(apiKey));

        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("X-API-Key", testKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("MERCHANT_NOT_APPROVED"))
                .andExpect(jsonPath("$.message").value("Merchant is not approved"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }

    @Test
    @DisplayName("Rejected merchant returns 403 with MERCHANT_NOT_APPROVED code")
    void rejectedMerchant_returns403() throws Exception {
        String testKey = "sdk_rejected_dummy_key_123456789012345678";
        String testHash = "rejectedHash";

        Merchant rejectedMerchant = Merchant.builder()
                .id(77L)
                .businessName("Rejected Shop")
                .status(MerchantStatus.REJECTED)
                .build();

        ApiKey apiKey = ApiKey.builder()
                .id(300L)
                .merchant(rejectedMerchant)
                .keyHash(testHash)
                .active(true)
                .build();

        when(apiKeyService.hashKey(testKey)).thenReturn(testHash);
        when(apiKeyService.findActiveKeyByHash(testHash)).thenReturn(Optional.of(apiKey));

        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("X-API-Key", testKey))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("MERCHANT_NOT_APPROVED"))
                .andExpect(jsonPath("$.message").value("Merchant is not approved"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }

    @Test
    @DisplayName("Bearer JWT-style token on /merchant-api/** without X-API-Key returns 401")
    void bearerJwtToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchant-api/test")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("Invalid or missing API key"))
                .andExpect(jsonPath("$.path").value("/api/v1/merchant-api/test"));
    }
}
