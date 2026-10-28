package com.appointment.api.notifications.service;

import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.config.NotificationProperties;
import com.appointment.api.notifications.dao.EmailOutbox;
import com.appointment.api.notifications.dao.OutboxStatus;
import com.appointment.api.notifications.repository.EmailOutboxRepository;
import com.appointment.api.notifications.sender.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/** Delivery is at-least-once: a crash between send and commit re-sends, so templates must tolerate repeats. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxProcessor {

    private final EmailOutboxRepository outbox;
    private final EmailSender sender;
    private final NotificationProperties properties;
    private final Clock clock;

    @Transactional
    public int processDue() {
        Instant now = clock.instant();
        var due = outbox.lockDue(now, properties.batchSize());
        for (EmailOutbox row : due) {
            deliver(row, now);
        }
        return due.size();
    }

    private void deliver(EmailOutbox row, Instant now) {
        row.setAttempts(row.getAttempts() + 1);
        try {
            sender.send(new EmailMessage(row.getDedupeKey(), row.getRecipient(), row.getTemplate(),
                    row.getVariables() == null ? Map.of() : row.getVariables()));
            row.setStatus(OutboxStatus.SENT);
            row.setSentAt(now);
            row.setVariables(null);
            row.setLastError(null);
        } catch (RuntimeException e) {
            log.error("Email {} attempt {} failed: {}", row.getId(), row.getAttempts(), e.getMessage());
            row.setLastError(truncate(e.getClass().getSimpleName()));
            if (row.getAttempts() >= properties.maxAttempts()) {
                row.setStatus(OutboxStatus.FAILED);
                row.setVariables(null);
                log.error("Email {} failed permanently after {} attempts", row.getId(), row.getAttempts());
            } else {
                long factor = 1L << Math.min(row.getAttempts() - 1, 10);
                row.setNextAttemptAt(now.plus(properties.baseBackoff().multipliedBy(factor)));
                log.warn("Email {} attempt {} failed, will retry", row.getId(), row.getAttempts());
            }
        }
    }

    private String truncate(String value) {
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
