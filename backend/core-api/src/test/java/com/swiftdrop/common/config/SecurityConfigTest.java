package com.swiftdrop.common.config;

import com.swiftdrop.auth.repository.UserRepository;
import com.swiftdrop.auth.security.JwtService;
import com.swiftdrop.auth.security.JwtTokenAuthenticationFilter;
import com.swiftdrop.auth.security.OAuth2AuthenticationSuccessHandler;
import com.swiftdrop.courier.service.CourierService;
import com.swiftdrop.merchant.security.ApiKeyAccessDeniedHandler;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationEntryPoint;
import com.swiftdrop.merchant.security.ApiKeyAuthenticationFilter;
import com.swiftdrop.security.JwtTokenProvider;
import com.swiftdrop.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CourierService courierService;

    @MockitoBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockitoBean
    private ApiKeyAuthenticationEntryPoint apiKeyAuthenticationEntryPoint;

    @MockitoBean
    private ApiKeyAccessDeniedHandler apiKeyAccessDeniedHandler;

    @Test
    void trackingAndWebsocketRoutesArePublic() throws Exception {
        mockMvc.perform(get("/api/v1/tracking/ABC123"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                String redirect = result.getResponse().getRedirectedUrl();
                assertThat(status).isNotIn(401, 403);
                assertThat(redirect).isNotEqualTo("http://localhost/oauth2/authorization/google");
            });
        mockMvc.perform(get("/ws/info"))
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                String redirect = result.getResponse().getRedirectedUrl();
                assertThat(status).isNotIn(401, 403);
                assertThat(redirect).isNotEqualTo("http://localhost/oauth2/authorization/google");
            });
    }

    @Test
    void unlistedRouteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    String redirect = result.getResponse().getRedirectedUrl();
                    assertThat(
                            status == 401
                                    || status == 403
                                    || (status == 302 && "http://localhost/oauth2/authorization/google".equals(redirect))
                    ).isTrue();
                });
    }
}
