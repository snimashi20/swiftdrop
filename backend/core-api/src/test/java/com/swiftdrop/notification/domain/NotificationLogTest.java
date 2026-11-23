package com.swiftdrop.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * core-api's copy of the notification_log mapping.
 *
 * <p>The notifications feature itself lives in the notification-service module, which has its own
 * NotificationLog and its own test for it. These classes stay here because core-api still maps the
 * same table; they carry the only branch in com.swiftdrop.notification.domain, the createdAt
 * default in {@link NotificationLog#onCreate()}, so both sides of it are covered here.
 */
class NotificationLogTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    @DisplayName("createdAt is stamped on persist when the row does not carry one")
    void onCreateStampsCreatedAt() {
        NotificationLog row = new NotificationLog();
        assertThat(row.getCreatedAt()).isNull();
        Instant before = Instant.now();

        row.onCreate();

        assertThat(row.getCreatedAt()).isNotNull().isBetween(before, Instant.now());
    }

    @Test
    @DisplayName("a createdAt already set is left alone on persist")
    void onCreateKeepsAnExistingCreatedAt() {
        Instant createdAt = Instant.parse("2026-11-01T08:00:00Z");
        NotificationLog row = NotificationLog.builder().createdAt(createdAt).build();

        row.onCreate();

        assertThat(row.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("the builder sets every column and the getters read them back")
    void builderSetsEveryField() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");
        Instant sentAt = Instant.parse("2026-11-14T10:15:31Z");

        NotificationLog row = NotificationLog.builder()
                .id(5L)
                .eventId(EVENT_ID)
                .parcelId(42L)
                .trackingNumber("SD7K2M9QX4LP")
                .oldStatus("PICKED_UP")
                .newStatus("IN_TRANSIT")
                .customerId(7L)
                .recipientUserId(9L)
                .recipientRole("MERCHANT")
                .type(NotificationType.EMAIL)
                .recipient("s***@example.com")
                .status(NotificationStatus.SENT)
                .failureReason(null)
                .createdAt(createdAt)
                .sentAt(sentAt)
                .build();

        assertThat(row.getId()).isEqualTo(5L);
        assertThat(row.getEventId()).isEqualTo(EVENT_ID);
        assertThat(row.getParcelId()).isEqualTo(42L);
        assertThat(row.getTrackingNumber()).isEqualTo("SD7K2M9QX4LP");
        assertThat(row.getOldStatus()).isEqualTo("PICKED_UP");
        assertThat(row.getNewStatus()).isEqualTo("IN_TRANSIT");
        assertThat(row.getCustomerId()).isEqualTo(7L);
        // The recipient is not always the customer: see migration V202611141100.
        assertThat(row.getRecipientUserId()).isEqualTo(9L);
        assertThat(row.getRecipientRole()).isEqualTo("MERCHANT");
        assertThat(row.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(row.getRecipient()).isEqualTo("s***@example.com");
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(row.getFailureReason()).isNull();
        assertThat(row.getCreatedAt()).isEqualTo(createdAt);
        assertThat(row.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    @DisplayName("the setters record the delivery outcome of a pending row")
    void settersRecordTheDeliveryOutcome() {
        NotificationLog row = new NotificationLog();
        row.setEventId(EVENT_ID);
        row.setType(NotificationType.SMS);
        row.setRecipient("*******567");
        row.setStatus(NotificationStatus.PENDING);
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.PENDING);

        Instant sentAt = Instant.parse("2026-11-14T10:15:31Z");
        row.setStatus(NotificationStatus.SENT);
        row.setSentAt(sentAt);
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(row.getSentAt()).isEqualTo(sentAt);

        row.setStatus(NotificationStatus.FAILED);
        row.setFailureReason("MailSendException");
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(row.getFailureReason()).isEqualTo("MailSendException");
    }

    @Test
    @DisplayName("the all-args constructor maps the columns in order")
    void allArgsConstructorMapsEveryColumn() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");

        NotificationLog row = new NotificationLog(5L, EVENT_ID, 42L, "SD7K2M9QX4LP", null, "CREATED",
                7L, 7L, "CUSTOMER", NotificationType.IN_APP, "s***@example.com",
                NotificationStatus.PENDING, null, createdAt, null);

        assertThat(row.getOldStatus()).isNull();
        assertThat(row.getNewStatus()).isEqualTo("CREATED");
        assertThat(row.getRecipientUserId()).isEqualTo(7L);
        assertThat(row.getRecipientRole()).isEqualTo("CUSTOMER");
        assertThat(row.getType()).isEqualTo(NotificationType.IN_APP);
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(row.getFailureReason()).isNull();
        assertThat(row.getCreatedAt()).isEqualTo(createdAt);
        assertThat(row.getSentAt()).isNull();
    }

    @Test
    @DisplayName("the channel and outcome enums hold exactly the values the log column allows")
    void enumsHoldTheValuesTheColumnAllows() {
        // notification-service writes this column, so its enum must stay in step with ours.
        assertThat(NotificationType.values())
                .containsExactly(NotificationType.IN_APP, NotificationType.EMAIL, NotificationType.SMS);
        assertThat(NotificationStatus.values())
                .containsExactly(NotificationStatus.PENDING, NotificationStatus.SENT, NotificationStatus.FAILED);
        assertThat(NotificationType.valueOf("EMAIL")).isEqualTo(NotificationType.EMAIL);
        assertThat(NotificationStatus.valueOf("PENDING")).isEqualTo(NotificationStatus.PENDING);
    }
}
