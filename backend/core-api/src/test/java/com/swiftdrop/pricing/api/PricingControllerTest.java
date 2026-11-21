package com.swiftdrop.pricing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftdrop.parcel.domain.ParcelSize;
import com.swiftdrop.pricing.dto.QuoteRequest;
import com.swiftdrop.pricing.dto.QuoteResponse;
import com.swiftdrop.pricing.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PricingControllerTest {

    @Mock
    private PricingService pricingService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        PricingController pricingController = new PricingController(pricingService);
        mockMvc = MockMvcBuilders.standaloneSetup(pricingController).build();
    }

    @Test
    void getQuote_returns200WithCalculatedFee() throws Exception {
        QuoteRequest request = new QuoteRequest();
        request.setPickupDistrict("Colombo");
        request.setDropoffDistrict("Kandy");
        request.setSize(ParcelSize.M);
        request.setWeightKg(new BigDecimal("2.0"));

        QuoteResponse response = QuoteResponse.builder()
                .fee(new BigDecimal("1000.00"))
                .pickupZone("Colombo Zone")
                .dropoffZone("Kandy Zone")
                .pickupZoneId(1L)
                .dropoffZoneId(3L)
                .isInterZone(true)
                .build();

        when(pricingService.calculateQuote(any(QuoteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(1000.00))
                .andExpect(jsonPath("$.interZone").value(true));
    }

    @Test
    void getQuote_returns400WhenWeightIsInvalid() throws Exception {
        QuoteRequest request = new QuoteRequest();
        request.setPickupDistrict("Colombo");
        request.setDropoffDistrict("Kandy");
        request.setSize(ParcelSize.M);
        request.setWeightKg(new BigDecimal("-1")); // Invalid weight

        // No mocking needed here. Spring's @Valid annotation automatically catches the negative weight and returns 400.
        mockMvc.perform(post("/api/v1/pricing/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
