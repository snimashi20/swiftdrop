package com.swiftdrop.ai.controller;

import com.swiftdrop.ai.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
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
        String reply = aiService.chat(message);
        return Map.of("reply", reply);
    }
}
