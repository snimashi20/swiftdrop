package com.swiftdrop.notification.service;

import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Component
public class EmailNotifier {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String from;
    private final String fromName;

    public EmailNotifier(JavaMailSender mailSender, ITemplateEngine templateEngine,
                         @Value("${app.mail.from}") String from,
                         @Value("${app.mail.from-name:SwiftDrop}") String fromName) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
        this.fromName = fromName;
    }

    /**
     * The subject line. A failure says what happened rather than naming a status, because that is
     * the one message the customer has to act on and the subject may be all they read.
     */
    public static String subject(ParcelStatusChangedEvent event) {
        if ("FAILED".equalsIgnoreCase(event.newStatus())) {
            return "SwiftDrop: we could not deliver parcel " + event.trackingNumber();
        }
        return "SwiftDrop: parcel " + event.trackingNumber() + " is now "
                + StatusText.readable(event.newStatus());
    }

    public void send(String to, ParcelStatusChangedEvent event) {
        Context context = new Context();
        context.setVariable("trackingNumber", event.trackingNumber());
        context.setVariable("oldStatus", event.oldStatus());
        context.setVariable("newStatus", event.newStatus());
        // Readable copies alongside the raw codes: the template still needs the codes for its
        // conditionals, but shows the words.
        context.setVariable("oldStatusText", StatusText.readable(event.oldStatus()));
        context.setVariable("newStatusText", StatusText.readable(event.newStatus()));
        context.setVariable("failureReason", StatusText.failureReason(event.failureReason()));
        context.setVariable("note", event.note());
        String html = templateEngine.process("status-changed-email", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            // The display name is what a mail client shows instead of the bare address. Blank means
            // the sender has no name to show, so fall back to the address on its own.
            if (fromName == null || fromName.isBlank()) {
                helper.setFrom(from);
            } else {
                helper.setFrom(from, fromName);
            }
            helper.setTo(to);
            helper.setSubject(subject(event));
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("Could not build status email", e);
        }
    }
}
