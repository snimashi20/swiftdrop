package com.swiftdrop.merchant.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swiftdrop.common.exception.GlobalExceptionHandler;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.common.exception.ValidationException;
import com.swiftdrop.merchant.domain.ApiKey;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantOrderResponse;
import com.swiftdrop.merchant.security.ApiKeyAccessDeniedHandler;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationEntryPoint;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationFilter;
import com.swiftdrop.merchant.service.ApiKeyService;
import com.swiftdrop.merchant.service.MerchantOrderService;
import java.util.Optional;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        MerchantOrderControllerMockMvcTest.TestConfig.class,
        MerchantOrderController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class MerchantOrderControllerMockMvcTest {
    private static final String API_KEY = "sdk_test_key";
    private static final String HASH = "hash";
    private static final String BODY = """
            {"pickup":{"line1":"pickup","city":"city","district":"district"},
             "dropoff":{"line1":"dropoff","city":"city","district":"district"},
             "receiverName":"receiver","receiverPhone":"0771234567","size":"M","weightKg":1}
            """;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean ApiKeyService apiKeyService() { return Mockito.mock(ApiKeyService.class); }
        @Bean MerchantOrderService merchantOrderService() { return Mockito.mock(MerchantOrderService.class); }

        @Bean
        ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(ApiKeyService service) {
            return new ApiKeyAuthenticationFilter(service, new ApiKeyAuthenticationEntryPoint(),
                    new ApiKeyAccessDeniedHandler());
        }

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http,
                                        ApiKeyAuthenticationFilter apiKeyFilter) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    private MockMvc mockMvc;
    @Autowired WebApplicationContext context;
    @Autowired ApiKeyService apiKeyService;
    @Autowired MerchantOrderService orderService;

    @BeforeEach
    void setUp() {
        reset(apiKeyService, orderService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Merchant merchant = Merchant.builder().id(7L).status(MerchantStatus.APPROVED).build();
        ApiKey apiKey = ApiKey.builder().id(3L).merchant(merchant).keyHash(HASH).active(true).build();
        when(apiKeyService.hashKey(API_KEY)).thenReturn(HASH);
        when(apiKeyService.findActiveKeyByHash(HASH)).thenReturn(Optional.of(apiKey));
        when(orderService.create(eq(7L), any(), any()))
                .thenReturn(new MerchantOrderService.CreationResult(order("SDABCDEFGHIJ"), true));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void missingIdempotencyKeyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-api/orders")
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
    }

    @Test
    void newOrderIsCreatedAndRepeatIsOk() throws Exception {
        when(orderService.create(eq(7L), eq("key"), any()))
                .thenReturn(new MerchantOrderService.CreationResult(order("SDABCDEFGHIJ"), true))
                .thenReturn(new MerchantOrderService.CreationResult(order("SDABCDEFGHIJ"), false));

        mockMvc.perform(post("/api/v1/merchant-api/orders").header("X-API-Key", API_KEY)
                        .header("Idempotency-Key", "key").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.trackingNumber").value("SDABCDEFGHIJ"));
        mockMvc.perform(post("/api/v1/merchant-api/orders").header("X-API-Key", API_KEY)
                        .header("Idempotency-Key", "key").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trackingNumber").value("SDABCDEFGHIJ"));
    }

    @Test
    void missingOrWrongApiKeyIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-api/orders").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        when(apiKeyService.findActiveKeyByHash("wrong-hash")).thenReturn(Optional.empty());
        when(apiKeyService.hashKey("wrong")).thenReturn("wrong-hash");
        mockMvc.perform(post("/api/v1/merchant-api/orders").header("X-API-Key", "wrong")
                        .header("Idempotency-Key", "key").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingMerchantIsForbidden() throws Exception {
        ApiKey pendingKey = ApiKey.builder().id(4L)
                .merchant(Merchant.builder().id(8L).status(MerchantStatus.PENDING).build())
                .keyHash("pending").active(true).build();
        when(apiKeyService.hashKey("pending")).thenReturn("pending");
        when(apiKeyService.findActiveKeyByHash("pending")).thenReturn(Optional.of(pendingKey));

        mockMvc.perform(post("/api/v1/merchant-api/orders").header("X-API-Key", "pending")
                        .header("Idempotency-Key", "key").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownDistrictIsValidationError() throws Exception {
        when(orderService.create(eq(7L), eq("bad-district"), any()))
                .thenThrow(new ValidationException("Unknown pickup or dropoff district"));

        mockMvc.perform(post("/api/v1/merchant-api/orders").header("X-API-Key", API_KEY)
                        .header("Idempotency-Key", "bad-district")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void anotherMerchantsTrackingNumberIsNotFound() throws Exception {
        when(orderService.get(7L, "SDOTHER1234")).thenThrow(new NotFoundException("Order not found"));
        mockMvc.perform(get("/api/v1/merchant-api/orders/SDOTHER1234").header("X-API-Key", API_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkOverFiftyItemsIsBadRequest() throws Exception {
        String item = BODY.trim();
        String body = "[" + String.join(",", java.util.Collections.nCopies(51, item)) + "]";
        mockMvc.perform(post("/api/v1/merchant-api/orders/bulk").header("X-API-Key", API_KEY)
                        .header("Idempotency-Key", "batch").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void pageAndSizeLimitsAreBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/merchant-api/orders?page=-1&size=20").header("X-API-Key", API_KEY))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/merchant-api/orders?page=0&size=101").header("X-API-Key", API_KEY))
                .andExpect(status().isBadRequest());
    }

    private MerchantOrderResponse order(String trackingNumber) {
        return MerchantOrderResponse.builder().trackingNumber(trackingNumber).build();
    }
}
