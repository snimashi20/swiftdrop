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

    boolean existsByEventIdAndRecipientUserIdAndType(UUID eventId, Long recipientUserId, NotificationType type);

    List<NotificationLog> findByEventId(UUID eventId);

    Optional<NotificationLog> findByIdAndRecipientUserId(Long id, Long recipientUserId);

    /**
     * One recipient's own feed, newest first, with one item per status change.
     *
     * <p>A status change writes one row per channel <em>per recipient</em>, so the lowest id of the
     * event among that recipient's own rows stands for the whole change. The subquery is scoped to
     * the recipient for exactly that reason: without it, every recipient would collapse onto the
     * customer's row, which is normally the lowest id of the event. Pass an unsorted Pageable: the
     * order is part of the query.
     */
    @Query(value = """
            SELECT n FROM NotificationLog n
            WHERE n.recipientUserId = :userId
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2
                           WHERE n2.eventId = n.eventId AND n2.recipientUserId = n.recipientUserId)
            ORDER BY n.createdAt DESC, n.id DESC
            """,
           countQuery = """
            SELECT COUNT(n) FROM NotificationLog n
            WHERE n.recipientUserId = :userId
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2
                           WHERE n2.eventId = n.eventId AND n2.recipientUserId = n.recipientUserId)
            """)
    Page<NotificationLog> findRecipientFeed(@Param("userId") Long userId, Pageable pageable);

    /** As {@link #findRecipientFeed} but only the status changes the recipient has not seen. */
    @Query(value = """
            SELECT n FROM NotificationLog n
            WHERE n.recipientUserId = :userId
              AND n.readAt IS NULL
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2
                           WHERE n2.eventId = n.eventId AND n2.recipientUserId = n.recipientUserId)
            ORDER BY n.createdAt DESC, n.id DESC
            """,
           countQuery = """
            SELECT COUNT(n) FROM NotificationLog n
            WHERE n.recipientUserId = :userId
              AND n.readAt IS NULL
              AND n.id = (SELECT MIN(n2.id) FROM NotificationLog n2
                           WHERE n2.eventId = n.eventId AND n2.recipientUserId = n.recipientUserId)
            """)
    Page<NotificationLog> findUnreadRecipientFeed(@Param("userId") Long userId, Pageable pageable);

    /** Counts status changes, not rows, so the bell agrees with the feed. */
    @Query("""
            SELECT COUNT(DISTINCT n.eventId) FROM NotificationLog n
            WHERE n.recipientUserId = :userId AND n.readAt IS NULL
            """)
    long countUnreadEvents(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationLog n SET n.readAt = :now
            WHERE n.recipientUserId = :userId AND n.readAt IS NULL
            """)
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Marks every channel of one status change for this recipient, so a half-read event cannot
     * happen. Another recipient's rows for the same event are left alone.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE NotificationLog n SET n.readAt = :now
            WHERE n.eventId = :eventId AND n.recipientUserId = :userId AND n.readAt IS NULL
            """)
    int markEventRead(@Param("eventId") UUID eventId,
                      @Param("userId") Long userId,
                      @Param("now") Instant now);
}
