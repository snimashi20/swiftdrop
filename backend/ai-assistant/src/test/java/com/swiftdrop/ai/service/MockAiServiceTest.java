package com.swiftdrop.ai.service;

import com.swiftdrop.ai.tool.SwiftDropTools;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class MockAiServiceTest {

    private final SwiftDropTools tools = Mockito.mock(SwiftDropTools.class);
    private final MockAiService service = new MockAiService(tools);

    @Test
    void detectsTrackingNumberAndCallsTool() {
        when(tools.getParcelStatus(eq("SDDEMO000001"))).thenReturn("PICKED_UP");
        String reply = service.chat("Where is my parcel SDDEMO000001?");
        assertTrue(reply.contains("SDDEMO000001"));
        assertTrue(reply.contains("PICKED_UP"));
    }

    @Test
    void detectsFeeQuestionAndCallsQuoteTool() {
        when(tools.getQuote(eq("Colombo"), eq("Kandy"), eq("M"), Mockito.anyDouble()))
                .thenReturn("{\"fee\":1000.00}");
        String reply = service.chat("How much to send 2kg from Colombo to Kandy?");
        assertTrue(reply.contains("1000.00"));
    }

    @Test
    void fallbackForUnknownQuestions() {
        String reply = service.chat("Hello");
        assertTrue(reply.contains("track a parcel"));
    }
}
