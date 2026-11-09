package com.swiftdrop.notification.repository;

import com.swiftdrop.notification.domain.NotificationLog;
import com.swiftdrop.notification.domain.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    boolean existsByEventIdAndType(UUID eventId, NotificationType type);

    List<NotificationLog> findByEventId(UUID eventId);

    Optional<NotificationLog> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * The customer's own feed, newest first, with one item per status change.
     *
     * <p>A status change writes one row per channel, so the lowest id of each event stands for the
     * whole change. Pass an unsorted Pageable: the order is part of the query.
     */
    @Query(value = """
            SELECT n FROM NotificationLog n
            WHERE n.customerId = :customerId
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2 WHERE n2.eventId = n.eventId)
            ORDER BY n.createdAt DESC, n.id DESC
            """,
           countQuery = """
            SELECT COUNT(n) FROM NotificationLog n
            WHERE n.customerId = :customerId
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2 WHERE n2.eventId = n.eventId)
            """)
    Page<NotificationLog> findCustomerFeed(@Param("customerId") Long customerId, Pageable pageable);

    /** As {@link #findCustomerFeed} but only the status changes the customer has not seen. */
    @Query(value = """
            SELECT n FROM NotificationLog n
            WHERE n.customerId = :customerId
              AND n.readAt IS NULL
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2 WHERE n2.eventId = n.eventId)
            ORDER BY n.createdAt DESC, n.id DESC
            """,
           countQuery = """
            SELECT COUNT(n) FROM NotificationLog n
            WHERE n.customerId = :customerId
              AND n.readAt IS NULL
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2 WHERE n2.eventId = n.eventId)
            """)
    Page<NotificationLog> findUnreadCustomerFeed(@Param("customerId") Long customerId, Pageable pageable);

    /** Counts status changes, not rows, so the bell agrees with the feed. */
    @Query("""
            SELECT COUNT(DISTINCT n.eventId) FROM NotificationLog n
            WHERE n.customerId = :customerId AND n.readAt IS NULL
            """)
    long countUnreadEvents(@Param("customerId") Long customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationLog n SET n.readAt = :now
            WHERE n.customerId = :customerId AND n.readAt IS NULL
            """)
    int markAllRead(@Param("customerId") Long customerId, @Param("now") Instant now);

    /** Marks every channel of one status change, so a half-read event cannot happen. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationLog n SET n.readAt = :now
            WHERE n.eventId = :eventId AND n.customerId = :customerId AND n.readAt IS NULL
            """)
    int markEventRead(@Param("eventId") UUID eventId,
                      @Param("customerId") Long customerId,
                      @Param("now") Instant now);
}
