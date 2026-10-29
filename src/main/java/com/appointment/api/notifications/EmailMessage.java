package com.appointment.api.notifications;

import com.appointment.api.notifications.template.EmailTemplate;

import java.util.Map;

public record EmailMessage(String dedupeKey, String recipient, EmailTemplate template, Map<String, String> variables) {
}
