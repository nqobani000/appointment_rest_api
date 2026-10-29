package com.appointment.api.notifications.repository;

import com.appointment.api.notifications.dao.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    boolean existsByDedupeKey(String dedupeKey);

    /** SKIP LOCKED lets several instances drain the queue without sending the same row twice. */
    @Query(value = """
            SELECT * FROM email_outbox
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<EmailOutbox> lockDue(@Param("now") Instant now, @Param("limit") int limit);
}
