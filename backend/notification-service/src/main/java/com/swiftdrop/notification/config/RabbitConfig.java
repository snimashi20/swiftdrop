package com.swiftdrop.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declarations mirror core-api's RabbitMQConfig exactly (same arguments), so whichever service starts first creates
// them and the other's declare is a no-op. Changing one side without the other makes RabbitMQ reject the declare.
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "swiftdrop.parcel.events";
    public static final String ROUTING_KEY = "parcel.status.changed";
    public static final String NOTIFICATION_QUEUE = "notification.parcel-status";
    public static final String NOTIFICATION_DLQ = "notification.parcel-status.dlq";

    @Bean
    public TopicExchange parcelEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
