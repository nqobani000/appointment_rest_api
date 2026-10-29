package com.appointment.api.notifications.sender;

import com.appointment.api.notifications.EmailMessage;

public interface EmailSender {

    /** Throws on failure so the outbox retries. */
    void send(EmailMessage message);
}
