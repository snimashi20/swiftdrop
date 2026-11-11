package com.swiftdrop.ai.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class SwiftDropTools {

    private final RestClient restClient;
    private final String coreApiUrl;

    public SwiftDropTools(RestClient restClient, @Value("${core-api.base-url}") String coreApiUrl) {
        this.restClient = restClient;
        this.coreApiUrl = coreApiUrl;
    }

    public String getParcelStatus(String trackingNumber) {
        try {
            return restClient.get()
                    .uri(coreApiUrl + "/api/v1/tracking/{trackingNumber}", trackingNumber)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "I could not find a parcel with tracking number " + trackingNumber + ". Please check the number.";
        }
    }

    public String getQuote(String pickupDistrict, String dropoffDistrict, String size, double weightKg) {
        try {
            Map<String, Object> body = Map.of(
                    "pickupDistrict", pickupDistrict,
                    "dropoffDistrict", dropoffDistrict,
                    "size", size,
                    "weightKg", weightKg
            );
            return restClient.post()
                    .uri(coreApiUrl + "/api/v1/pricing/quote")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "I could not calculate a fee for " + pickupDistrict + " to " + dropoffDistrict + ". Please check the district names.";
        }
    }
}
