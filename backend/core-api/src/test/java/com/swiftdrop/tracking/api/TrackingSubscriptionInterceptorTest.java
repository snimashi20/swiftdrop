package com.swiftdrop.tracking.api;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackingSubscriptionInterceptorTest {

    private final WebSocketConfig.TrackingSubscriptionInterceptor interceptor =
            new WebSocketConfig.TrackingSubscriptionInterceptor();

    private Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void allowsSubscribingToOneExactTrackingTopic() {
        assertDoesNotThrow(() ->
                interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/tracking/SD7K2M9QX4LP"), null));
    }

    @Test
    void refusesWildcardAndOtherTopics() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/tracking/*"), null));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/tracking/**"), null));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/other"), null));
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null), null));
    }

    @Test
    void refusesClientsSendingMessages() {
        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(frame(StompCommand.SEND, "/topic/tracking/SD7K2M9QX4LP"), null));
    }

    @Test
    void letsConnectThrough() {
        assertDoesNotThrow(() -> interceptor.preSend(frame(StompCommand.CONNECT, null), null));
    }
}
