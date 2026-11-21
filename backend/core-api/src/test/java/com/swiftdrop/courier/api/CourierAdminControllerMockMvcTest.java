package com.swiftdrop.courier.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.common.exception.ConflictException;
import com.swiftdrop.common.exception.GlobalExceptionHandler;
import com.swiftdrop.common.exception.NotFoundException;
import com.swiftdrop.courier.dto.CourierApplicationResponseDto;
import com.swiftdrop.courier.service.CourierApplicationService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CourierAdminControllerMockMvcTest.TestConfig.class,
        CourierAdminController.class,
        GlobalExceptionHandler.class
})
@WebAppConfiguration
class CourierAdminControllerMockMvcTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestConfig {
        @Bean
        CourierApplicationService courierApplicationService() {
            return Mockito.mock(CourierApplicationService.class);
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
    private CourierApplicationService courierApplicationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(courierApplicationService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void admin_canApprove() throws Exception {
        when(courierApplicationService.approve(eq(1L), any()))
                .thenReturn(CourierApplicationResponseDto.builder().id(1L).build());

        mockMvc.perform(post("/api/v1/admin/couriers/1/approve")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MERCHANT", "CUSTOMER", "COURIER", "DISPATCHER"})
    void nonAdmin_cannotApprove(String role) throws Exception {
        mockMvc.perform(post("/api/v1/admin/couriers/1/approve")
                        .with(user(role.toLowerCase() + "@example.com").roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void unauthenticated_cannotApprove() throws Exception {
        mockMvc.perform(post("/api/v1/admin/couriers/1/approve"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MERCHANT", "CUSTOMER", "COURIER", "DISPATCHER"})
    void nonAdmin_cannotListApplications(String role) throws Exception {
        mockMvc.perform(get("/api/v1/admin/couriers")
                        .with(user(role.toLowerCase() + "@example.com").roles(role)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_rejectRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/admin/couriers/1/reject")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("reason", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_rejectRejectsReasonOver500Characters() throws Exception {
        mockMvc.perform(post("/api/v1/admin/couriers/1/reject")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("reason", "a".repeat(501)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/admin/couriers?page=-1")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/couriers?size=101")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void admin_acceptsValidPagination() throws Exception {
        when(courierApplicationService.listForAdmin(any(), any()))
                .thenReturn(new PageImpl<>(
                        java.util.List.of(CourierApplicationResponseDto.builder().id(1L).build()),
                        PageRequest.of(1, 100),
                        1));

        mockMvc.perform(get("/api/v1/admin/couriers?page=1&size=100")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_reviewErrorsUseSharedStatuses() throws Exception {
        when(courierApplicationService.approve(eq(1L), any()))
                .thenThrow(new ConflictException("Only pending courier applications can be reviewed"));
        when(courierApplicationService.reject(eq(2L), eq("reason"), any()))
                .thenThrow(new NotFoundException("Courier application not found"));

        mockMvc.perform(post("/api/v1/admin/couriers/1/approve")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/admin/couriers/2/reject")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"reason\"}"))
                .andExpect(status().isNotFound());
    }
}
