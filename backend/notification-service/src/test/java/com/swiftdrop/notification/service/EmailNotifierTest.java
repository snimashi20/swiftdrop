package com.swiftdrop.notification.service;

import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The e-mail channel: template rendering, the MIME message it builds, and build failures. */
@ExtendWith(MockitoExtension.class)
class EmailNotifierTest {

    private static final String FROM = "no-reply@swiftdrop.local";
    private static final String TO = "sithmi@example.com";

    @Mock JavaMailSender mailSender;
    @Mock ITemplateEngine templateEngine;

    EmailNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new EmailNotifier(mailSender, templateEngine, FROM);
    }

    @Test
    @DisplayName("the subject carries the tracking number and the new status")
    void subjectNamesTrackingNumberAndNewStatus() {
        assertThat(EmailNotifier.subject(event("PICKED_UP", "IN_TRANSIT")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now IN_TRANSIT");
    }

    @Test
    @DisplayName("the subject is the same standard line for a new parcel and for a cancellation")
    void subjectHasNoSpecialCaseForCreationOrCancellation() {
        assertThat(EmailNotifier.subject(event(null, "CREATED")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now CREATED");
        assertThat(EmailNotifier.subject(event("IN_TRANSIT", "CANCELLED")))
                .isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now CANCELLED");
    }

    @Test
    @DisplayName("a valid recipient gets an HTML mail with the rendered template, from, to and subject set")
    void sendsRenderedHtmlToTheRecipient() throws Exception {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(templateEngine.process(eq("status-changed-email"), any(IContext.class)))
                .thenReturn("<p>Parcel SD7K2M9QX4LP is now IN_TRANSIT</p>");

        notifier.send(TO, event("PICKED_UP", "IN_TRANSIT"));

        verify(mailSender).send(message);
        assertThat(message.getFrom()[0].toString()).isEqualTo(FROM);
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo(TO);
        assertThat(message.getSubject()).isEqualTo("SwiftDrop: parcel SD7K2M9QX4LP is now IN_TRANSIT");
        // The Content-Type header is only written on saveChanges(), so read the body's own handler.
        assertThat(message.getDataHandler().getContentType()).contains("text/html");
        assertThat((String) message.getContent()).contains("Parcel SD7K2M9QX4LP is now IN_TRANSIT");
    }

    @Test
    @DisplayName("the template receives the tracking number, both statuses and the note")
    void passesEventFieldsToTheTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(eq("status-changed-email"), any(IContext.class))).thenReturn("<p>hi</p>");

        notifier.send(TO, event("PICKED_UP", "IN_TRANSIT"));

        ArgumentCaptor<IContext> context = ArgumentCaptor.captor();
        verify(templateEngine).process(eq("status-changed-email"), context.capture());
        assertThat(context.getValue().getVariable("trackingNumber")).isEqualTo("SD7K2M9QX4LP");
        assertThat(context.getValue().getVariable("oldStatus")).isEqualTo("PICKED_UP");
        assertThat(context.getValue().getVariable("newStatus")).isEqualTo("IN_TRANSIT");
        assertThat(context.getValue().getVariable("note")).isEqualTo("On the way");
    }

    @Test
    @DisplayName("a first event passes a null oldStatus to the template instead of failing")
    void passesNullOldStatusToTheTemplate() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(eq("status-changed-email"), any(IContext.class))).thenReturn("<p>hi</p>");

        notifier.send(TO, event(null, "CREATED"));

        ArgumentCaptor<Context> context = ArgumentCaptor.captor();
        verify(templateEngine).process(eq("status-changed-email"), context.capture());
        assertThat(context.getValue().getVariable("oldStatus")).isNull();
        assertThat(context.getValue().getVariable("newStatus")).isEqualTo("CREATED");
    }

    @Test
    @DisplayName("an unparseable recipient address becomes a MailPreparationException and nothing is sent")
    void invalidRecipientAddressIsRejectedWithoutSending() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(eq("status-changed-email"), any(IContext.class))).thenReturn("<p>hi</p>");

        assertThatThrownBy(() -> notifier.send("<no-closing-bracket@example.com", event("PICKED_UP", "IN_TRANSIT")))
                .isInstanceOf(MailPreparationException.class)
                .hasMessageContaining("Could not build status email")
                .hasCauseInstanceOf(MessagingException.class);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("a MIME failure while building the message becomes a MailPreparationException")
    void mimeFailureBecomesMailPreparationException() throws Exception {
        MimeMessage broken = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(broken);
        when(templateEngine.process(eq("status-changed-email"), any(IContext.class))).thenReturn("<p>hi</p>");
        doThrow(new MessagingException("header rejected")).when(broken).setFrom(any(Address.class));

        assertThatThrownBy(() -> notifier.send(TO, event("PICKED_UP", "IN_TRANSIT")))
                .isInstanceOf(MailPreparationException.class)
                .hasCauseInstanceOf(MessagingException.class);

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private static ParcelStatusChangedEvent event(String oldStatus, String newStatus) {
        return new ParcelStatusChangedEvent(UUID.randomUUID(), 42L, "SD7K2M9QX4LP", oldStatus, newStatus,
                Instant.parse("2026-11-14T10:15:30Z"), "COURIER", 7L, 99L, "On the way");
    }
}
