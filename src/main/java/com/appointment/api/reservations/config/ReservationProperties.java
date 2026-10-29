package com.appointment.api.reservations.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.ZoneId;

/** zone: the zone in which consultant working hours (local times) are interpreted. */
@ConfigurationProperties(prefix = "app.reservation")
public record ReservationProperties(
        @DefaultValue("PT10M") Duration holdDuration,
        @DefaultValue("Africa/Johannesburg") ZoneId zone,
        @DefaultValue("PT1M") Duration cleanupInterval) {
}
