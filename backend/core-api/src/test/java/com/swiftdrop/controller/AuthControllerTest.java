package com.swiftdrop.controller;

import com.swiftdrop.dto.auth.AuthResponse;
import com.swiftdrop.dto.auth.RegisterRequest;
import com.swiftdrop.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token", "Bearer", 1L, "user@example.com", "User", Set.of("ROLE_CUSTOMER")));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"User\",\"email\":\"user@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void loginReturnsOk() throws Exception {
        when(authService.login(any(com.swiftdrop.dto.auth.LoginRequest.class)))
                .thenReturn(new AuthResponse("token", "Bearer", 1L, "user@example.com", "User", Set.of("ROLE_CUSTOMER")));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }
}
