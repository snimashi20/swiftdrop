package com.swiftdrop.ai.controller;

import com.swiftdrop.ai.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class ChatController {

    private final AiService aiService;

    public ChatController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, Object> body) {
        String message = "";
        
        // Safe casting check
        if (body.containsKey("messages") && body.get("messages") instanceof List) {
            List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
            StringBuilder contextBuilder = new StringBuilder();
            int userMsgCount = 0;

            for (int i = messages.size() - 1; i >= 0; i--) {
                if ("user".equals(messages.get(i).get("role"))) {
                    contextBuilder.insert(0, messages.get(i).get("content") + " ");
                    userMsgCount++;
                    if (userMsgCount >= 2) break;
                }
            }
            message = contextBuilder.toString().trim();
        } 
        else if (body.containsKey("message") && body.get("message") instanceof String) {
            message = (String) body.get("message");
        }

        if (message == null || message.isBlank()) {
            return Map.of("reply", "Please ask a question.");
        }
        
        return Map.of("reply", aiService.chat(message));
    }
}
