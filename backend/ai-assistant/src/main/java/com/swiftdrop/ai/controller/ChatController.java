package com.swiftdrop.ai.controller;

import com.swiftdrop.ai.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return Map.of("reply", "Please ask a question.");
        }
        return Map.of("reply", aiService.chat(message));
    }
}
