package com.swiftdrop.tracking.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.regex.Pattern;

/** STOMP over a plain WebSocket: endpoint /ws, topic /topic/tracking/{trackingNumber}. */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // Origins come from the environment (.env), never hard-coded.
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new TrackingSubscriptionInterceptor());
    }

    /**
     * Browsers may only SUBSCRIBE to one exact tracking topic. They cannot send messages
     * (no forged updates) and cannot subscribe with a wildcard (no listening to every parcel).
     */
    static class TrackingSubscriptionInterceptor implements ChannelInterceptor {

        private static final Pattern ALLOWED_TOPIC = Pattern.compile("^/topic/tracking/SD[A-Z0-9]{10}$");

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            StompCommand command = accessor.getCommand();
            if (StompCommand.SEND.equals(command)) {
                throw new MessageDeliveryException("Clients cannot send messages");
            }
            if (StompCommand.SUBSCRIBE.equals(command)) {
                String destination = accessor.getDestination();
                if (destination == null || !ALLOWED_TOPIC.matcher(destination).matches()) {
                    throw new MessageDeliveryException("Subscription not allowed");
                }
            }
            return message;
        }
    }
}
