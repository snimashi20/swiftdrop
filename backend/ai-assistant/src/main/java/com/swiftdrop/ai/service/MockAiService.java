package com.swiftdrop.ai.service;

import com.swiftdrop.ai.tool.SwiftDropTools;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiService implements AiService {

    private final SwiftDropTools tools;

    // Matches "SD" + 10 alphanumeric chars (the contract format)
    private static final Pattern TRACKING = Pattern.compile("\\b(SD[A-Z0-9]{10})\\b", Pattern.CASE_INSENSITIVE);
    // Matches weight like "2kg" or "2.5 kg"
    private static final Pattern WEIGHT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*kg", Pattern.CASE_INSENSITIVE);
    // Extracts the fee from the JSON response
    private static final Pattern FEE_PATTERN = Pattern.compile("\"fee\":(\\d+(?:\\.\\d+)?)");

    public MockAiService(SwiftDropTools tools) {
        this.tools = tools;
    }

    @Override
    public String chat(String message) {
        String lower = message.toLowerCase();

        // 1. If the message has a tracking number → call tracking API
        Matcher trackingMatcher = TRACKING.matcher(message);
        if (trackingMatcher.find()) {
            String trackingNumber = trackingMatcher.group(1).toUpperCase();
            String rawStatus = tools.getParcelStatus(trackingNumber);
            
            // If M5's API returns a JSON error or plain string, we just return it cleanly
            if (rawStatus.contains("not found") || rawStatus.contains("Could not find")) {
                return "I could not find a parcel with tracking number " + trackingNumber + ". Please check the number.";
            }
            return "Here is the latest status for parcel " + trackingNumber + ":\n\n" + rawStatus;
        }

        // 2. If the message mentions a fee/price/quote → try to call pricing API
        if (lower.contains("cost") || lower.contains("price") || lower.contains("fee")
                || lower.contains("quote") || lower.contains("how much")) {
            return handleQuote(message);
        }

        // 3. Fallback
        return "Hi! I am the SwiftDrop Assistant. I can help you track a parcel (give me a tracking number like SD7K2M9QX4LP) "
                + "or estimate a delivery fee (say something like 'how much to send 2kg from Colombo to Kandy').";
    }

    private String handleQuote(String message) {
        Matcher weightMatcher = WEIGHT.matcher(message);
        if (!weightMatcher.find()) {
            return "Please tell me the weight in kg (e.g., '2kg').";
        }
        double weight = Double.parseDouble(weightMatcher.group(1));

        // Try to find two districts mentioned
        String[] districts = {
            "Ampara", "Anuradhapura", "Badulla", "Batticaloa", "Colombo", 
            "Galle", "Gampaha", "Hambantota", "Jaffna", "Kalutara", 
            "Kandy", "Kegalle", "Kilinochchi", "Kurunegala", "Mannar", 
            "Matale", "Matara", "Monaragala", "Mullaitivu", "Nuwara Eliya", 
            "Polonnaruwa", "Puttalam", "Ratnapura", "Trincomalee", "Vavuniya"
        };
        
        String from = null, to = null;
        for (String d : districts) {
            if (message.toLowerCase().contains(d.toLowerCase())) {
                if (from == null) from = d;
                else if (to == null) to = d;
            }
        }

        if (from == null || to == null) {
            return "Please tell me the pickup and dropoff districts (e.g., Colombo, Kandy, Galle).";
        }

        // Get the raw JSON from the pricing API
        String jsonResponse = tools.getQuote(from, to, "M", weight);

        // Extract the fee to make the response look human-readable
        Matcher feeMatcher = FEE_PATTERN.matcher(jsonResponse);
        if (feeMatcher.find()) {
            String fee = feeMatcher.group(1);
            return "The estimated delivery fee for a " + weight + "kg parcel from " + from + " to " + to + " is LKR " + fee + ".";
        }

        // Fallback if parsing fails
        return "Here is the estimate for " + weight + "kg from " + from + " to " + to + ":\n" + jsonResponse;
    }
}
