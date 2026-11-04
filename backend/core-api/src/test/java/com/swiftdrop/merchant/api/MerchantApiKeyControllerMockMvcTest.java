package com.swiftdrop.merchant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.exception.GlobalExceptionHandler;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.ApiKeyGeneratedResponse;
import com.swiftdrop.merchant.dto.ApiKeyResponse;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.merchant.service.ApiKeyService;
import com.swiftdrop.merchant.exception.MerchantNotApprovedException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        MerchantApiKeyControllerMockMvcTest.TestConfig.class,
        MerchantApiKeyController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class MerchantApiKeyControllerMockMvcTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        ApiKeyService apiKeyService() {
            return Mockito.mock(ApiKeyService.class);
        }

        @Bean
        CurrentMerchantProvider currentMerchantProvider() {
            return Mockito.mock(CurrentMerchantProvider.class);
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                            .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN)))
                    .build();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private CurrentMerchantProvider currentMerchantProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(apiKeyService, currentMerchantProvider);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pendingMerchant_cannotCreateKey() throws Exception {
        Merchant merchant = Merchant.builder().id(1L).status(MerchantStatus.PENDING).build();
        when(currentMerchantProvider.getCurrentMerchant(any())).thenReturn(merchant);
        when(apiKeyService.generate(eq(1L), eq("test"))).thenThrow(new MerchantNotApprovedException("Merchant is not approved"));

        mockMvc.perform(post("/api/v1/merchants/me/api-keys")
                        .with(user("merchant@example.com").roles("MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("label", "test"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MERCHANT_NOT_APPROVED"));
    }

    @Test
    void customer_cannotCreateKey() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/me/api-keys")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void unauthenticated_cannotCreateKey() throws Exception {
        mockMvc.perform(post("/api/v1/merchants/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void merchant_cannotRevokeAnotherMerchantsKey() throws Exception {
        Merchant merchant = Merchant.builder().id(1L).status(MerchantStatus.APPROVED).build();
        when(currentMerchantProvider.getCurrentMerchant(any())).thenReturn(merchant);
        when(apiKeyService.revoke(1L, 99L))
                .thenThrow(new com.swiftdrop.common.exception.NotFoundException("API key not found"));

        mockMvc.perform(post("/api/v1/merchants/me/api-keys/99/revoke")
                        .with(user("merchant@example.com").roles("MERCHANT")))
                .andExpect(status().isNotFound());
    }

    @Test
    void approvedMerchant_canCreateKey() throws Exception {
        Merchant merchant = Merchant.builder().id(1L).status(MerchantStatus.APPROVED).build();
        when(currentMerchantProvider.getCurrentMerchant(any())).thenReturn(merchant);
        when(apiKeyService.generate(1L, "test")).thenReturn(new ApiKeyGeneratedResponse(
                2L, 1L, "test", "sdk_prefix", "sdk_plain_key", true, null));

        mockMvc.perform(post("/api/v1/merchants/me/api-keys")
                        .with(user("merchant@example.com").roles("MERCHANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"test\"}"))
                .andExpect(status().isCreated());
    }
}
