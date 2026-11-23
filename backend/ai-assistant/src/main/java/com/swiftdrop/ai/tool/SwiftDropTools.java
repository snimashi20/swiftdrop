package com.swiftdrop.ai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SwiftDropTools {

    private final RestClient restClient;
    private final String coreApiUrl;

    private static final List<String> VALID_DISTRICTS = List.of(
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo",
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara",
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar",
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya",
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
    );

    private static final Map<String, String> LOCAL_DISTRICT_OVERRIDES = Map.ofEntries(
            Map.entry("kelaniya", "Gampaha"),
            Map.entry("ragama", "Gampaha"),
            Map.entry("wattala", "Gampaha"),
            Map.entry("ja-ela", "Gampaha"),
            Map.entry("nugegoda", "Colombo"),
            Map.entry("dehiwala", "Colombo"),
            Map.entry("moratuwa", "Colombo"),
            Map.entry("avissawella", "Colombo"),
            Map.entry("awissawella", "Colombo"),
            Map.entry("negombo", "Gampaha"),
            Map.entry("peradeniya", "Kandy"),
            Map.entry("katugastota", "Kandy"),
            Map.entry("nuwaraeliya", "Nuwara Eliya")
    );

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
            log.warn("Failed to retrieve parcel status for tracking number: {}", trackingNumber, e);
            return "I could not find a parcel with tracking number " + trackingNumber + ". Please check the number.";
        }
    }

    public String getQuote(String pickupDistrict, String dropoffDistrict, String size, double weightKg) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pickupDistrict", pickupDistrict);
            body.put("dropoffDistrict", dropoffDistrict);
            body.put("weightKg", weightKg);
            body.put("size", size != null ? size : "S");

            return restClient.post()
                    .uri(coreApiUrl + "/api/v1/pricing/quote")
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Quote API Error for {} to {}", pickupDistrict, dropoffDistrict, e);
            return "I could not calculate a fee for " + pickupDistrict + " to " + dropoffDistrict + ". The server responded with an error.";
        }
    }

    public String resolveDistrict(String city) {
        String lowerCity = city.toLowerCase().trim();

        if (LOCAL_DISTRICT_OVERRIDES.containsKey(lowerCity)) {
            return LOCAL_DISTRICT_OVERRIDES.get(lowerCity);
        }

        try {
            String url = "https://nominatim.openstreetmap.org/search?q={city},Sri+Lanka&format=json&addressdetails=1&limit=3";
            
            List<Map<String, Object>> response = restClient.get()
                    .uri(url, city)
                    .header("User-Agent", "SwiftDrop-App-Demo")
                    .retrieve()
                    .body(List.class);

            if (response == null || response.isEmpty()) {
                String globalUrl = "https://nominatim.openstreetmap.org/search?q={city}&format=json&limit=1";
                List<Map<String, Object>> globalResponse = restClient.get()
                        .uri(globalUrl, city)
                        .header("User-Agent", "SwiftDrop-App-Demo")
                        .retrieve()
                        .body(List.class);
                
                if (globalResponse != null && !globalResponse.isEmpty()) {
                    return "Outside Sri Lanka";
                }
                return city;
            }

            for (Map<String, Object> result : response) {
                Map<String, Object> address = (Map<String, Object>) result.get("address");
                if (address != null) {
                    String rawDistrict = null;
                    if (address.containsKey("state_district")) {
                        rawDistrict = address.get("state_district").toString().replace(" District", "").trim();
                    } else if (address.containsKey("county")) {
                        rawDistrict = address.get("county").toString().replace(" District", "").trim();
                    }

                    if (rawDistrict != null) {
                        for (String valid : VALID_DISTRICTS) {
                            if (valid.equalsIgnoreCase(rawDistrict)) {
                                return valid;
                            }
                        }
                    }
                }
            }
            
            return city;
        } catch (Exception e) {
            log.error("Geocoding failed for: {}", city, e);
            return city;
        }
    }
}
