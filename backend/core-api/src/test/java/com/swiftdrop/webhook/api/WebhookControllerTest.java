package com.swiftdrop.webhook.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.swiftdrop.webhook.domain.DeliveryStatus;
import com.swiftdrop.webhook.dto.TestPingResponse;
import com.swiftdrop.webhook.dto.UpdateWebhookEndpointRequest;
import com.swiftdrop.webhook.dto.WebhookDeliveryDto;
import com.swiftdrop.webhook.dto.WebhookEndpointDto;
import com.swiftdrop.webhook.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    @DisplayName("GET /api/v1/webhooks/endpoint should return merchant endpoint config")
    void shouldGetEndpoint() throws Exception {
        WebhookEndpointDto dto = WebhookEndpointDto.builder()
                .id(1L)
                .merchantId(101L)
                .url("http://example.com/webhook")
                .secret("whsec_test123")
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(webhookService.resolveCurrentMerchantId()).thenReturn(101L);
        when(webhookService.getEndpoint(101L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/webhooks/endpoint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.merchantId").value(101))
                .andExpect(jsonPath("$.url").value("http://example.com/webhook"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/webhooks/endpoint should update and return endpoint config")
    void shouldUpdateEndpoint() throws Exception {
        UpdateWebhookEndpointRequest request = UpdateWebhookEndpointRequest.builder()
                .url("https://merchant.com/webhook")
                .active(true)
                .build();

        WebhookEndpointDto updatedDto = WebhookEndpointDto.builder()
                .id(1L)
                .merchantId(101L)
                .url("https://merchant.com/webhook")
                .secret("whsec_secret")
                .active(true)
                .build();

        when(webhookService.resolveCurrentMerchantId()).thenReturn(101L);
        when(webhookService.updateOrCreateEndpoint(eq(101L), any(UpdateWebhookEndpointRequest.class)))
                .thenReturn(updatedDto);

        mockMvc.perform(put("/api/v1/webhooks/endpoint")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://merchant.com/webhook"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/test-ping should return ping diagnosis")
    void shouldSendTestPing() throws Exception {
        TestPingResponse pingResponse = TestPingResponse.builder()
                .success(true)
                .httpStatus(200)
                .message("Ping succeeded")
                .durationMs(45)
                .build();

        when(webhookService.resolveCurrentMerchantId()).thenReturn(101L);
        when(webhookService.sendTestPing(101L)).thenReturn(pingResponse);

        mockMvc.perform(post("/api/v1/webhooks/test-ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.httpStatus").value(200))
                .andExpect(jsonPath("$.durationMs").value(45));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/deliveries/{id}/resend should trigger manual resend")
    void shouldResendDelivery() throws Exception {
        WebhookDeliveryDto resendDto = WebhookDeliveryDto.builder()
                .id(42L)
                .status(DeliveryStatus.SUCCESS)
                .attempts(2)
                .trackingNumber("SD7K2M9QX4LP")
                .build();

        when(webhookService.resendDelivery(42L)).thenReturn(resendDto);

        mockMvc.perform(post("/api/v1/webhooks/deliveries/42/resend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.attempts").value(2));
    }
}
