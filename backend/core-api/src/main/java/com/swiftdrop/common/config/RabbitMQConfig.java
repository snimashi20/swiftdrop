package com.swiftdrop.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "swiftdrop.parcel.events";
    public static final String ROUTING_KEY = "parcel.status.changed";

    public static final String NOTIFICATION_QUEUE = "notification.parcel-status";
    public static final String NOTIFICATION_DLQ = "notification.parcel-status.dlq";

    public static final String WEBHOOK_QUEUE = "webhook.parcel-status";
    public static final String WEBHOOK_DLQ = "webhook.parcel-status.dlq";

    @Bean
    public TopicExchange parcelEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    // --- Notification queue + dead-letter ---

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange parcelEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(parcelEventsExchange).with(ROUTING_KEY);
    }

    // --- Webhook queue + dead-letter ---

    @Bean
    public Queue webhookDeadLetterQueue() {
        return QueueBuilder.durable(WEBHOOK_DLQ).build();
    }

    @Bean
    public Queue webhookQueue() {
        return QueueBuilder.durable(WEBHOOK_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", WEBHOOK_DLQ)
                .build();
    }

    @Bean
    public Binding webhookBinding(Queue webhookQueue, TopicExchange parcelEventsExchange) {
        return BindingBuilder.bind(webhookQueue).to(parcelEventsExchange).with(ROUTING_KEY);
    }
}
