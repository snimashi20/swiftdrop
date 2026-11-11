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
            return "Here is the status for " + trackingNumber + ":\n" + tools.getParcelStatus(trackingNumber);
        }

        // 2. If the message mentions a fee/price/quote → try to call pricing API
        if (lower.contains("cost") || lower.contains("price") || lower.contains("fee")
                || lower.contains("quote") || lower.contains("how much")) {
            return handleQuote(message);
        }

        // 3. Fallback
        return "Hi! I can help you track a parcel (give me a tracking number like SD7K2M9QX4LP) "
                + "or estimate a delivery fee (say something like 'how much to send 2kg from Colombo to Kandy').";
    }

    private String handleQuote(String message) {
        Matcher weightMatcher = WEIGHT.matcher(message);
        if (!weightMatcher.find()) {
            return "Please tell me the weight in kg (e.g., '2kg').";
        }
        double weight = Double.parseDouble(weightMatcher.group(1));

        // Try to find two districts mentioned
        String[] districts = {"Colombo", "Gampaha", "Kandy", "Galle", "Jaffna"};
        String from = null, to = null;
        for (String d : districts) {
            if (message.toLowerCase().contains(d.toLowerCase())) {
                if (from == null) from = d;
                else if (to == null) to = d;
            }
        }

        if (from == null || to == null) {
            return "Please tell me the pickup and dropoff districts (Colombo, Gampaha, Kandy, Galle, Jaffna).";
        }

        return "Here is the estimate for " + weight + "kg from " + from + " to " + to + ":\n"
                + tools.getQuote(from, to, "M", weight);
    }
}
