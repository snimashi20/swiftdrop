package com.swiftdrop.ai;

import com.swiftdrop.ai.controller.ChatController;
import com.swiftdrop.ai.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    private AiService aiService;
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        aiService = Mockito.mock(AiService.class);
        chatController = new ChatController(aiService);
    }

    @Test
    void testChatWithMessageString() {
        when(aiService.chat(anyString())).thenReturn("Mocked reply");
        
        Map<String, Object> body = Map.of("message", "Hello");
        Map<String, String> response = chatController.chat(body);
        
        assertEquals("Mocked reply", response.get("reply"));
    }

    @Test
    void testChatWithMessagesList() {
        when(aiService.chat("Hello world ")).thenReturn("Mocked list reply");
        
        Map<String, Object> body = Map.of("messages", List.of(
                Map.of("role", "user", "content", "Hello"),
                Map.of("role", "assistant", "content", "Hi"),
                Map.of("role", "user", "content", "world")
        ));
        
        Map<String, String> response = chatController.chat(body);
        assertEquals("Mocked list reply", response.get("reply"));
    }

    @Test
    void testChatWithInvalidBody() {
        Map<String, Object> body = Map.of("invalidKey", 123);
        Map<String, String> response = chatController.chat(body);
        
        assertEquals("Please ask a question.", response.get("reply"));
    }
}
