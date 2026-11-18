package com.swiftdrop.notification.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The mock SMS gateway. Nothing leaves the system, so the behaviour that matters is the text it
 * builds and the fact that it only ever logs a masked number.
 */
class SmsNotifierTest {

    private final SmsNotifier notifier = new SmsNotifier();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureLogs() {
        logger = (Logger) LoggerFactory.getLogger(SmsNotifier.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    @DisplayName("the text carries the tracking number and the new status")
    void textNamesTrackingNumberAndNewStatus() {
        assertThat(SmsNotifier.text(event("PICKED_UP", "IN_TRANSIT")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now IN_TRANSIT");
    }

    @Test
    @DisplayName("the text is the same standard line for a new parcel and for a cancellation")
    void textHasNoSpecialCaseForCreationOrCancellation() {
        assertThat(SmsNotifier.text(event(null, "CREATED")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now CREATED");
        assertThat(SmsNotifier.text(event("IN_TRANSIT", "CANCELLED")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now CANCELLED");
    }

    @Test
    @DisplayName("a valid number is logged masked, never in full")
    void logsTheMaskedNumberOnly() {
        notifier.send("0771234567", event("PICKED_UP", "IN_TRANSIT"));

        assertThat(appender.list).hasSize(1);
        String line = appender.list.get(0).getFormattedMessage();
        assertThat(line).isEqualTo("[MOCK SMS] to *******567: SwiftDrop: parcel SD7K2M9QX4LP is now IN_TRANSIT");
        assertThat(line).doesNotContain("0771234567");
    }

    @Test
    @DisplayName("a number too short to mask is sent as *** without crashing")
    void tooShortNumberIsFullyMasked() {
        assertThatCode(() -> notifier.send("12", event("PICKED_UP", "IN_TRANSIT")))
                .doesNotThrowAnyException();

        assertThat(appender.list.get(0).getFormattedMessage()).startsWith("[MOCK SMS] to ***:");
    }

    @Test
    @DisplayName("a number written with spaces is masked over its digits only")
    void spacedNumberIsMasked() {
        notifier.send("077 123 4567", event("PICKED_UP", "IN_TRANSIT"));

        assertThat(appender.list.get(0).getFormattedMessage()).startsWith("[MOCK SMS] to *******567:");
    }

    private static ParcelStatusChangedEvent event(String oldStatus, String newStatus) {
        return new ParcelStatusChangedEvent(UUID.randomUUID(), 42L, "SD7K2M9QX4LP", oldStatus, newStatus,
                Instant.parse("2026-11-14T10:15:30Z"), "COURIER", 7L, 99L, "On the way");
    }
}
