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

@Component
public class EmailNotifier {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final String from;

    public EmailNotifier(JavaMailSender mailSender, ITemplateEngine templateEngine,
                         @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
    }

    public static String subject(ParcelStatusChangedEvent event) {
        return "SwiftDrop: parcel " + event.trackingNumber() + " is now " + event.newStatus();
    }

    public void send(String to, ParcelStatusChangedEvent event) {
        Context context = new Context();
        context.setVariable("trackingNumber", event.trackingNumber());
        context.setVariable("oldStatus", event.oldStatus());
        context.setVariable("newStatus", event.newStatus());
        context.setVariable("note", event.note());
        String html = templateEngine.process("status-changed-email", context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject(event));
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build status email", e);
        }
    }
}
