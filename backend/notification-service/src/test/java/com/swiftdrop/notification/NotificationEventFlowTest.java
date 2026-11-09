package com.swiftdrop.notification;

import com.swiftdrop.notification.config.RabbitConfig;
import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationStatus;
import com.swiftdrop.notification.domain.NotificationType;
import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import com.swiftdrop.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("docker")
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class NotificationEventFlowTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Container
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3-management");

    @Container
    static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/livez").forPort(8025));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired NotificationLogRepository logs;
    @Autowired JdbcTemplate jdbc;

    @Test
    void statusEventSendsEmailToMailpitAndLogsBothChannels() throws Exception {
        Long customerId = createCustomer("0771234567");
        ParcelStatusChangedEvent event = event(customerId, "SD7K2M9QX4LP");

        // Same type header core-api's JacksonJsonMessageConverter attaches; the consumer must not depend on it.
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event, message -> {
            message.getMessageProperties().setHeader("__TypeId__", "com.swiftdrop.common.event.ParcelStatusChangedEvent");
            return message;
        });

        await().atMost(Duration.ofSeconds(15)).until(() -> sentRows(event.eventId()) == 2);

        Map<NotificationType, NotificationLog> byType = logs.findByEventId(event.eventId()).stream()
                .collect(Collectors.toMap(NotificationLog::getType, Function.identity()));
        assertThat(byType.get(NotificationType.EMAIL).getRecipient()).matches("[a-z]\\*\\*\\*@example\\.com");
        assertThat(byType.get(NotificationType.SMS).getRecipient()).isEqualTo("*******567");

        await().atMost(Duration.ofSeconds(10))
                .until(() -> mailpitMessages().contains("SwiftDrop: parcel SD7K2M9QX4LP is now IN_TRANSIT"));
    }

    @Test
    void duplicateEventIdIsIgnored() {
        Long customerId = createCustomer("0712345678");
        ParcelStatusChangedEvent event = event(customerId, "SDDUPL1CATE1");

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event);

        await().atMost(Duration.ofSeconds(15)).until(() -> sentRows(event.eventId()) == 2);
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .until(() -> logs.findByEventId(event.eventId()).size() == 2);
    }

    @Test
    void poisonMessageLandsInDeadLetterQueue() {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbitTemplate.send(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                new Message("this is not json".getBytes(StandardCharsets.UTF_8), props));

        Message dead = rabbitTemplate.receive(RabbitConfig.NOTIFICATION_DLQ, 15_000);

        assertThat(dead).isNotNull();
        assertThat(new String(dead.getBody(), StandardCharsets.UTF_8)).isEqualTo("this is not json");
    }

    private Long createCustomer(String phone) {
        String email = "c" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        return jdbc.queryForObject(
                "INSERT INTO users (email, full_name, phone_number) VALUES (?, ?, ?) RETURNING id",
                Long.class, email, "Test Customer", phone);
    }

    private static ParcelStatusChangedEvent event(Long customerId, String trackingNumber) {
        return new ParcelStatusChangedEvent(UUID.randomUUID(), 42L, trackingNumber, "PICKED_UP", "IN_TRANSIT",
                Instant.now(), "COURIER", customerId, null, "On the way");
    }

    private long sentRows(UUID eventId) {
        List<NotificationLog> rows = logs.findByEventId(eventId);
        return rows.stream().filter(r -> r.getStatus() == NotificationStatus.SENT).count();
    }

    private static String mailpitMessages() throws Exception {
        URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + "/api/v1/messages");
        return HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString())
                .body();
    }
}
