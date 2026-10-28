package com.appointment.api.notifications.service;

import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.NotificationService;
import com.appointment.api.notifications.dao.EmailOutbox;
import com.appointment.api.notifications.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final EmailOutboxRepository outbox;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean enqueue(EmailMessage message) {
        if (outbox.existsByDedupeKey(message.dedupeKey())) {
            return false;
        }
        Instant now = clock.instant();
        EmailOutbox row = new EmailOutbox();
        row.setDedupeKey(message.dedupeKey());
        row.setRecipient(message.recipient());
        row.setTemplate(message.template());
        row.setVariables(new HashMap<>(message.variables()));
        row.setCreatedAt(now);
        row.setNextAttemptAt(now);
        outbox.save(row);
        return true;
    }
}
