package com.swiftdrop.dispatch.api;

import com.swiftdrop.dispatch.dto.AssignParcelRequest;
import com.swiftdrop.dispatch.service.CourierProfileService;
import com.swiftdrop.dispatch.service.DispatchService;
import com.swiftdrop.dispatch.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Required authorization test: only DISPATCHER and ADMIN may use the dispatch endpoints.
 * A small Spring context with method security switched on checks the @PreAuthorize rules;
 * the services are mocks, so no database is needed.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DispatchAuthorizationTest.TestConfig.class)
class DispatchAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean DispatchService dispatchService() { return mock(DispatchService.class); }
        @Bean ReportService reportService() { return mock(ReportService.class); }
        @Bean CourierProfileService courierProfileService() { return mock(CourierProfileService.class); }

        @Bean DispatchController dispatchController(DispatchService service) {
            return new DispatchController(service);
        }

        @Bean ReportController reportController(ReportService service) {
            return new ReportController(service);
        }

        @Bean CourierProfileController courierProfileController(CourierProfileService service) {
            return new CourierProfileController(service);
        }
    }

    @Autowired DispatchController dispatchController;
    @Autowired ReportController reportController;
    @Autowired CourierProfileController courierProfileController;

    @AfterEach
    void clearLogin() {
        SecurityContextHolder.clearContext();
    }

    private static void loginAs(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "someone@swiftdrop.lk", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Executable assign() {
        return () -> dispatchController.assign(5L, new AssignParcelRequest(7L), null);
    }

    private List<Executable> allEndpoints() {
        return List.of(
                assign(),
                () -> dispatchController.unassigned(null, null, null, null, Pageable.unpaged()),
                () -> dispatchController.workload(null, null),
                () -> dispatchController.suggestedCourier(5L),
                () -> reportController.summary(7),
                () -> courierProfileController.list(null, null, Pageable.unpaged()),
                () -> courierProfileController.get(1L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MERCHANT", "COURIER"})
    void assign_isForbiddenForOtherRoles(String role) {
        loginAs(role);

        assertThrows(AccessDeniedException.class, assign());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPATCHER", "ADMIN"})
    void assign_isAllowedForDispatcherAndAdmin(String role) {
        loginAs(role);

        assertDoesNotThrow(assign());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MERCHANT", "COURIER"})
    void everyDispatchEndpointIsForbiddenForOtherRoles(String role) {
        loginAs(role);

        for (Executable endpoint : allEndpoints()) {
            assertThrows(AccessDeniedException.class, endpoint);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"DISPATCHER", "ADMIN"})
    void everyDispatchEndpointIsOpenToDispatcherAndAdmin(String role) {
        loginAs(role);

        for (Executable endpoint : allEndpoints()) {
            assertDoesNotThrow(endpoint);
        }
    }

    @Test
    void anonymousUserCannotUseDispatchEndpoints() {
        for (Executable endpoint : allEndpoints()) {
            assertThrows(RuntimeException.class, endpoint);
        }
    }
}