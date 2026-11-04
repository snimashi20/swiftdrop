package com.swiftdrop.common.event;

import com.swiftdrop.common.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringJUnitConfig(ParcelStatusEventPublisherTest.Config.class)
class ParcelStatusEventPublisherTest {

    @Configuration
    @EnableTransactionManagement
    @Import(ParcelStatusEventPublisher.class)
    static class Config {
        @Bean
        RabbitTemplate rabbitTemplate() {
            return Mockito.mock(RabbitTemplate.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoOpTransactionManager();
        }
    }

    // Real transaction synchronization without a database, so AFTER_COMMIT/rollback behave as in production.
    static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }

    @Autowired ApplicationEventPublisher events;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    private TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        Mockito.reset(rabbitTemplate);
        tx = new TransactionTemplate(transactionManager);
    }

    @Test
    void sendsToRabbitOnlyAfterCommit() {
        ParcelStatusChangedEvent event = event();

        tx.executeWithoutResult(status -> {
            events.publishEvent(event);
            verifyNoInteractions(rabbitTemplate);
        });

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }

    @Test
    void sendsNothingWhenTransactionRollsBack() {
        tx.executeWithoutResult(status -> {
            events.publishEvent(event());
            status.setRollbackOnly();
        });

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void brokerOutageDoesNotFailTheCommittedChange() {
        doThrow(new AmqpConnectException(new ConnectException("down")))
                .when(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.ROUTING_KEY), any(Object.class));

        assertThatCode(() -> tx.executeWithoutResult(status -> events.publishEvent(event())))
                .doesNotThrowAnyException();
    }

    private static ParcelStatusChangedEvent event() {
        return new ParcelStatusChangedEvent(UUID.randomUUID(), 1L, "SD7K2M9QX4LP", "PICKED_UP", "IN_TRANSIT",
                Instant.now(), "COURIER", 7L, null, null);
    }
}
