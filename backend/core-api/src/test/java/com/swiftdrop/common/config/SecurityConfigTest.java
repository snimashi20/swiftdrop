package com.swiftdrop.common.config;

import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.service.AuthService;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.security.JwtTokenAuthenticationFilter;
import com.swiftdrop.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = true)
@WebMvcTest
@ContextConfiguration(classes = {
    SecurityConfig.class,
    SecurityProbeController.class,
    JwtTokenProvider.class,
    JwtTokenAuthenticationFilter.class
})
@TestPropertySource(properties = "app.jwt.secret=01234567890123456789012345678901")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CourierService courierService;

    @Test
    void trackingAndWebsocketRoutesArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/ABC123"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/ws/info"))
            .andExpect(status().isNotFound());
    }

    @Test
    void unlistedRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
                });
    }
}
