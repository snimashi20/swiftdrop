package com.swiftdrop.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The notification_log mapping: the builder, the accessors and the createdAt default. */
class NotificationLogTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    @DisplayName("the builder sets every column and the getters read them back")
    void builderSetsEveryField() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");
        Instant sentAt = Instant.parse("2026-11-14T10:15:31Z");
        Instant readAt = Instant.parse("2026-11-14T11:00:00Z");

        NotificationLog row = NotificationLog.builder()
                .id(5L)
                .eventId(EVENT_ID)
                .parcelId(42L)
                .trackingNumber("SD7K2M9QX4LP")
                .oldStatus("PICKED_UP")
                .newStatus("IN_TRANSIT")
                .customerId(7L)
                .type(NotificationType.EMAIL)
                .recipient("s***@example.com")
                .status(NotificationStatus.SENT)
                .failureReason(null)
                .createdAt(createdAt)
                .sentAt(sentAt)
                .readAt(readAt)
                .build();

        assertThat(row.getId()).isEqualTo(5L);
        assertThat(row.getEventId()).isEqualTo(EVENT_ID);
        assertThat(row.getParcelId()).isEqualTo(42L);
        assertThat(row.getTrackingNumber()).isEqualTo("SD7K2M9QX4LP");
        assertThat(row.getOldStatus()).isEqualTo("PICKED_UP");
        assertThat(row.getNewStatus()).isEqualTo("IN_TRANSIT");
        assertThat(row.getCustomerId()).isEqualTo(7L);
        assertThat(row.getType()).isEqualTo(NotificationType.EMAIL);
        assertThat(row.getRecipient()).isEqualTo("s***@example.com");
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(row.getFailureReason()).isNull();
        assertThat(row.getCreatedAt()).isEqualTo(createdAt);
        assertThat(row.getSentAt()).isEqualTo(sentAt);
        assertThat(row.getReadAt()).isEqualTo(readAt);
    }

    @Test
    @DisplayName("a new row carries no id, no timestamps and counts as unread")
    void newRowIsEmptyAndUnread() {
        NotificationLog row = new NotificationLog();

        assertThat(row.getId()).isNull();
        assertThat(row.getCreatedAt()).isNull();
        assertThat(row.getSentAt()).isNull();
        assertThat(row.getReadAt()).isNull();
        assertThat(row.getFailureReason()).isNull();
    }

    @Test
    @DisplayName("the setters move a pending row to SENT and to FAILED")
    void settersRecordTheDeliveryOutcome() {
        NotificationLog row = NotificationLog.builder()
                .eventId(EVENT_ID)
                .type(NotificationType.SMS)
                .recipient("*******567")
                .status(NotificationStatus.PENDING)
                .build();
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.PENDING);

        Instant sentAt = Instant.parse("2026-11-14T10:15:31Z");
        row.setStatus(NotificationStatus.SENT);
        row.setSentAt(sentAt);
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(row.getSentAt()).isEqualTo(sentAt);

        row.setStatus(NotificationStatus.FAILED);
        row.setFailureReason("MailSendException");
        row.setReadAt(Instant.parse("2026-11-14T11:00:00Z"));
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(row.getFailureReason()).isEqualTo("MailSendException");
        assertThat(row.getReadAt()).isEqualTo(Instant.parse("2026-11-14T11:00:00Z"));
    }

    @Test
    @DisplayName("the all-args constructor maps the columns in order")
    void allArgsConstructorMapsEveryColumn() {
        Instant createdAt = Instant.parse("2026-11-14T10:15:30Z");

        NotificationLog row = new NotificationLog(5L, EVENT_ID, 42L, "SD7K2M9QX4LP", null, "CREATED", 7L,
                7L, "CUSTOMER", NotificationType.EMAIL, "s***@example.com", NotificationStatus.PENDING,
                null, "RECIPIENT_NOT_AVAILABLE", createdAt, null, null);

        assertThat(row.getOldStatus()).isNull();
        assertThat(row.getNewStatus()).isEqualTo("CREATED");
        assertThat(row.getRecipientUserId()).isEqualTo(7L);
        assertThat(row.getRecipientRole()).isEqualTo("CUSTOMER");
        // The two reason columns are distinct: this one is the parcel's, failureReason is the send's.
        assertThat(row.getParcelFailureReason()).isEqualTo("RECIPIENT_NOT_AVAILABLE");
        assertThat(row.getFailureReason()).isNull();
        assertThat(row.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(row.getCreatedAt()).isEqualTo(createdAt);
        assertThat(row.getReadAt()).isNull();
    }

    @Test
    @DisplayName("createdAt is stamped on persist when the row does not carry one")
    void onCreateStampsCreatedAt() {
        NotificationLog row = new NotificationLog();
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
    @DisplayName("the channel and outcome enums hold exactly the values the log column allows")
    void enumsHoldTheExpectedValues() {
        assertThat(NotificationType.values())
                .containsExactly(NotificationType.IN_APP, NotificationType.EMAIL, NotificationType.SMS);
        assertThat(NotificationStatus.valueOf("PENDING")).isEqualTo(NotificationStatus.PENDING);
        assertThat(NotificationStatus.values())
                .contains(NotificationStatus.PENDING, NotificationStatus.SENT, NotificationStatus.FAILED);
    }
}
