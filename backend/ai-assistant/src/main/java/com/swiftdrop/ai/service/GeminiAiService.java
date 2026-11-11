package com.swiftdrop.ai.service;

import com.swiftdrop.ai.tool.SwiftDropTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
public class GeminiAiService implements AiService {

    private final ChatClient chatClient;

    public GeminiAiService(ChatClient.Builder builder, SwiftDropTools tools) {
        this.chatClient = builder
                .defaultSystem("""
                        You are SwiftDrop's helpful AI assistant. You help customers:
                        - Track parcels by tracking number (e.g., SD7K2M9QX4LP)
                        - Estimate delivery fees (need pickup district, dropoff district, weight in kg, size S/M/L)
                        Available districts: Colombo, Gampaha, Kandy, Galle, Jaffna.
                        Be concise and friendly. Use the tools to get real data.
                        """)
                .defaultTools(tools)
                .build();
    }

    @Override
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
