package com.swiftdrop.notification.repository;

import com.swiftdrop.notification.domain.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByEventIdAndType(UUID eventId, com.swiftdrop.notification.domain.NotificationType type);

    Page<NotificationLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
