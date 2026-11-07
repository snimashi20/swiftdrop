package com.swiftdrop.merchant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.GlobalExceptionHandler;
import com.swiftdrop.merchant.domain.MerchantStatus;
import com.swiftdrop.merchant.dto.MerchantResponseDto;
import com.swiftdrop.merchant.dto.RegisterMerchantRequest;
import com.swiftdrop.merchant.service.MerchantService;
import com.swiftdrop.merchant.service.MerchantOrderService;
import com.swiftdrop.merchant.security.CurrentMerchantProvider;
import com.swiftdrop.merchant.domain.Merchant;
import com.swiftdrop.parcel.domain.ParcelStatus;
import org.springframework.data.domain.PageImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        MerchantControllerMockMvcTest.TestConfig.class,
        MerchantController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class MerchantControllerMockMvcTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        public MerchantService merchantService() {
            return Mockito.mock(MerchantService.class);
        }

        @Bean
        public MerchantOrderService merchantOrderService() {
            return Mockito.mock(MerchantOrderService.class);
        }

        @Bean
        public CurrentMerchantProvider currentMerchantProvider() {
            return Mockito.mock(CurrentMerchantProvider.class);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/merchants/register").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                    .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                );
            return http.build();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MerchantService merchantService;
    @Autowired
    private MerchantOrderService merchantOrderService;
    @Autowired
    private CurrentMerchantProvider currentMerchantProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(merchantService, merchantOrderService, currentMerchantProvider);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- Register Endpoint Tests ---

    @Test
    @DisplayName("POST /merchants/register: returns 201 Created on valid input")
    void register_validInput_returns201() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("store@example.lk")
                .password("password123")
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .contactName("Kasun Perera")
                .contactPhone("0712345678")
                .build();

        MerchantResponseDto responseDto = MerchantResponseDto.builder()
                .id(1L)
                .userId(100L)
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .contactName("Kasun Perera")
                .contactEmail("store@example.lk")
                .contactPhone("0712345678")
                .status(MerchantStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(merchantService.register(any(RegisterMerchantRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.businessName").value("Store LK"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.contactEmail").value("store@example.lk"));
    }

    @Test
    @DisplayName("POST /merchants/register: returns 400 when phone number is invalid")
    void register_invalidPhone_returns400() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("store@example.lk")
                .password("password123")
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .contactName("Kasun Perera")
                .contactPhone("1234567890") // Invalid Sri Lankan mobile format
                .build();

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /merchants/register: returns 400 when required fields are missing")
    void register_missingFields_returns400() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("") // Blank email
                .password("") // Blank password
                .build();

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /merchants/register: returns 409 when email is already registered")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("store@example.lk")
                .password("password123")
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .contactName("Kasun Perera")
                .contactPhone("0712345678")
                .build();

        when(merchantService.register(any(RegisterMerchantRequest.class)))
                .thenThrow(new ConflictException("Email already in use"));

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email already in use"));
    }

    @Test
    @DisplayName("POST /merchants/register: returns 409 when registration number already exists")
    void register_duplicateRegistrationNumber_returns409() throws Exception {
        RegisterMerchantRequest request = RegisterMerchantRequest.builder()
                .email("store2@example.lk")
                .password("password123")
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .contactName("Kasun Perera")
                .contactPhone("0712345678")
                .build();

        when(merchantService.register(any(RegisterMerchantRequest.class)))
                .thenThrow(new ConflictException("Business registration number already registered"));

        mockMvc.perform(post("/api/v1/merchants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Business registration number already registered"));
    }

    // --- GET /merchants/me Endpoint Tests ---

    @Test
    @WithMockUser(username = "merchant@example.lk", roles = "MERCHANT")
    @DisplayName("GET /merchants/me: returns 200 OK for an authenticated MERCHANT")
    void getMyProfile_merchantRole_returns200() throws Exception {
        MerchantResponseDto profile = MerchantResponseDto.builder()
                .id(1L)
                .userId(100L)
                .businessName("Store LK")
                .registrationNumber("REG-999")
                .status(MerchantStatus.PENDING)
                .build();

        when(merchantService.getMyProfile(any(Authentication.class))).thenReturn(profile);

        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.businessName").value("Store LK"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "merchant@example.lk", roles = "MERCHANT")
    void dashboardOrders_areScopedToCurrentMerchantAndFilterable() throws Exception {
        Merchant merchant = Merchant.builder().id(41L).build();
        when(currentMerchantProvider.getCurrentMerchant(any())).thenReturn(merchant);
        when(merchantOrderService.listDashboard(eq(41L), eq(ParcelStatus.DELIVERED), any()))
                .thenReturn(new PageImpl<>(java.util.List.of()));

        mockMvc.perform(get("/api/v1/merchants/me/orders?status=DELIVERED&page=0&size=20"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(merchantOrderService).listDashboard(eq(41L), eq(ParcelStatus.DELIVERED), any());
    }

    @Test
    @WithMockUser(username = "customer@example.lk", roles = "CUSTOMER")
    void dashboardOrders_areMerchantOnly() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me/orders"))
                .andExpect(status().isForbidden());
        org.mockito.Mockito.verifyNoInteractions(merchantOrderService);
    }

    @Test
    @WithMockUser(username = "customer@example.lk", roles = "CUSTOMER")
    @DisplayName("GET /merchants/me: returns 403 Forbidden for other roles (e.g. CUSTOMER)")
    void getMyProfile_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "courier@example.lk", roles = "COURIER")
    @DisplayName("GET /merchants/me: returns 403 Forbidden for COURIER role")
    void getMyProfile_courierRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("GET /merchants/me: returns 401 Unauthorized when unauthenticated")
    void getMyProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me"))
                .andExpect(status().isUnauthorized());
    }
}
