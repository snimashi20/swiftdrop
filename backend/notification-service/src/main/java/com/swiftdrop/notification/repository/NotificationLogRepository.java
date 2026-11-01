package com.swiftdrop.notification.repository;

import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByEventIdAndType(UUID eventId, NotificationType type);

    List<NotificationLog> findByEventId(UUID eventId);
}
