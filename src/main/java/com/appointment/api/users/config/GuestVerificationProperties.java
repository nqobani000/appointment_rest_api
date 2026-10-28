package com.appointment.api.users.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.guest-verification")
public record GuestVerificationProperties(
        @DefaultValue("PT10M") Duration codeTtl,
        @DefaultValue("5") int maxAttempts,
        @DefaultValue("PT1M") Duration resendCooldown) {
}
