package com.swiftdrop.courier;

import com.swiftdrop.auth.domain.User;
import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.common.exception.BadRequestException;
import com.swiftdrop.common.exception.ForbiddenException;
import com.swiftdrop.common.exception.GlobalExceptionHandler;
import com.swiftdrop.courier.api.CourierController;
import com.swiftdrop.courier.dto.CourierDeliveryResponse;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.dispatch.domain.CourierProfile;
import com.swiftdrop.dispatch.repository.CourierProfileRepository;
import com.swiftdrop.tracking.service.InvalidStatusTransitionException;
import com.swiftdrop.parcel.domain.ParcelStatus;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CourierControllerMockMvcTest.TestConfig.class,
        CourierController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class CourierControllerMockMvcTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {

        @Bean
        public CourierService courierService() {
            return Mockito.mock(CourierService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        public CourierProfileRepository courierProfileRepository() {
            return Mockito.mock(CourierProfileRepository.class);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
    private CourierService courierService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(courierService, userRepository, courierProfileRepository);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User courierUser = new User();
        courierUser.setId(55L);
        courierUser.setEmail("courier@swiftdrop.lk");

        CourierProfile profile = new CourierProfile();
        profile.setId(101L);

        when(userRepository.findByEmail("courier@swiftdrop.lk")).thenReturn(Optional.of(courierUser));
        when(courierProfileRepository.findByUserId(55L)).thenReturn(Optional.of(profile));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/courier/deliveries/today without auth returns 401")
    void getTodayDeliveries_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/today"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "ADMIN", "DISPATCHER", "MERCHANT"})
    @DisplayName("GET /api/v1/courier/deliveries/today returns 403 for non-COURIER roles")
    void getTodayDeliveries_nonCourierRoles_returns403(String role) throws Exception {
        mockMvc.perform(get("/api/v1/courier/deliveries/today")
                        .with(user("other@swiftdrop.lk").roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/courier/deliveries/today returns 200 for COURIER role")
    void getTodayDeliveries_asCourier_returns200() throws Exception {
        CourierDeliveryResponse item = CourierDeliveryResponse.builder()
                .parcelId(99L)
                .trackingNumber("SDDEMO000002")
                .recipientName("Kamala Silva")
                .status("PICKED_UP")
                .build();
        when(courierService.getTodayDeliveries(101L)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/courier/deliveries/today")
                        .with(user("courier@swiftdrop.lk").roles("COURIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trackingNumber").value("SDDEMO000002"));
    }

    @Test
    @DisplayName("PATCH /api/v1/courier/deliveries/{id}/status returns 403 when updating another courier's parcel")
    void updateStatus_otherCouriersParcel_returns403() throws Exception {
        when(courierService.updateStatus(eq(101L), eq(55L), eq(99L), any()))
                .thenThrow(new ForbiddenException("Forbidden: You are not assigned to this parcel delivery"));

        mockMvc.perform(patch("/api/v1/courier/deliveries/99/status")
                        .with(user("courier@swiftdrop.lk").roles("COURIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/courier/deliveries/{id}/status returns 409 on invalid lifecycle transition")
    void updateStatus_invalidTransition_returns409() throws Exception {
        when(courierService.updateStatus(eq(101L), eq(55L), eq(99L), any()))
                .thenThrow(new InvalidStatusTransitionException(ParcelStatus.DELIVERED, ParcelStatus.PICKED_UP));

        mockMvc.perform(patch("/api/v1/courier/deliveries/99/status")
                        .with(user("courier@swiftdrop.lk").roles("COURIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PICKED_UP\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /api/v1/courier/deliveries/{id}/status returns 400 on unknown status string")
    void updateStatus_unknownStatus_returns400() throws Exception {
        when(courierService.updateStatus(eq(101L), eq(55L), eq(99L), any()))
                .thenThrow(new BadRequestException("Unknown parcel status: INVALID"));

        mockMvc.perform(patch("/api/v1/courier/deliveries/99/status")
                        .with(user("courier@swiftdrop.lk").roles("COURIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/courier/deliveries/{id}/deliver accepts multipart JSON + JPEG photo")
    void markDelivered_validMultipart_returns200() throws Exception {
        CourierDeliveryResponse delivered = CourierDeliveryResponse.builder()
                .parcelId(99L)
                .trackingNumber("SDDEMO000004")
                .status("DELIVERED")
                .proofPhotoUrl("/uploads/proofs/test.jpg")
                .build();
        when(courierService.deliverParcel(eq(101L), eq(55L), eq(99L), any(), any())).thenReturn(delivered);

        MockMultipartFile data = new MockMultipartFile(
                "data", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"recipientName\":\"Mrs. Silva\",\"note\":\"Left at gate\"}".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "proof.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/courier/deliveries/99/deliver")
                        .file(data)
                        .file(photo)
                        .with(user("courier@swiftdrop.lk").roles("COURIER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }
}