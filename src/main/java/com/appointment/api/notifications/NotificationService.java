package com.appointment.api.notifications;

public interface NotificationService {

    /** Queues the message in the caller's transaction; returns false when the dedupe key was already queued. */
    boolean enqueue(EmailMessage message);
}
