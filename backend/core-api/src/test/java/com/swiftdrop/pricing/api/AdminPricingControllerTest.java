package com.swiftdrop.pricing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.pricing.dto.PricingRuleRequest;
import com.swiftdrop.pricing.dto.PricingRuleResponse;
import com.swiftdrop.pricing.dto.ZoneRequest;
import com.swiftdrop.pricing.dto.ZoneResponse;
import com.swiftdrop.pricing.service.AdminPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminPricingControllerTest {

    @Mock
    private AdminPricingService adminPricingService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Standalone setup bypasses Spring Context and Security entirely
        AdminPricingController controller = new AdminPricingController(adminPricingService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllZones_returns200() throws Exception {
        when(adminPricingService.getAllZones()).thenReturn(List.of(
                ZoneResponse.builder().id(1L).name("Colombo").district("Colombo").isActive(true).build()
        ));

        mockMvc.perform(get("/api/v1/admin/pricing/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Colombo"));
    }

    @Test
    void createZone_returns201() throws Exception {
        ZoneRequest request = new ZoneRequest();
        request.setName("New Zone");
        request.setDistrict("New District");

        when(adminPricingService.createZone(any(ZoneRequest.class)))
                .thenReturn(ZoneResponse.builder().id(2L).name("New Zone").district("New District").isActive(true).build());

        mockMvc.perform(post("/api/v1/admin/pricing/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void updateRule_returns200() throws Exception {
        PricingRuleRequest request = new PricingRuleRequest();
        request.setZoneId(1L); // <--- THIS WAS MISSING
        request.setBaseFee(new BigDecimal("350.00"));
        request.setPerKgFee(new BigDecimal("120.00"));
        request.setInterZoneSurcharge(new BigDecimal("300.00"));

        when(adminPricingService.updateRule(eq(1L), any(PricingRuleRequest.class)))
                .thenReturn(PricingRuleResponse.builder()
                        .id(1L).zoneId(1L).zoneName("Colombo")
                        .baseFee(new BigDecimal("350.00"))
                        .perKgFee(new BigDecimal("120.00"))
                        .interZoneSurcharge(new BigDecimal("300.00"))
                        .build());

        mockMvc.perform(put("/api/v1/admin/pricing/rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseFee").value(350.00));
    }
}
