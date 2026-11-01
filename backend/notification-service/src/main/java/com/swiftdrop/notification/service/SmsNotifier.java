package com.swiftdrop.notification.service;

import com.swiftdrop.notification.event.ParcelStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Mock SMS gateway: nothing leaves the system; the masked number and status land in notification_log.
@Slf4j
@Component
public class SmsNotifier {

    public static String text(ParcelStatusChangedEvent event) {
        return "SwiftDrop: parcel " + event.trackingNumber() + " is now " + event.newStatus();
    }

    public void send(String phoneNumber, ParcelStatusChangedEvent event) {
        log.info("[MOCK SMS] to {}: {}", Masking.phone(phoneNumber), text(event));
    }
}
