package com.appointment.api.users.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration ttl) {

    public JwtProperties {
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalStateException("JWT_SECRET is not set. Define it in .env or the run configuration's environment variables.");
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt.secret (JWT_SECRET) must be at least 32 bytes.");
        }
    }
}
