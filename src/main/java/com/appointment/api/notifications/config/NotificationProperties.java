package com.appointment.api.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/** sender: "logging" (default, dev only) or "smtp". */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
        @DefaultValue("logging") String sender,
        @DefaultValue("no-reply@appointments.local") String from,
        @DefaultValue("PT10S") Duration pollInterval,
        @DefaultValue("20") int batchSize,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("PT30S") Duration baseBackoff) {
}
