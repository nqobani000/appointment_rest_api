package com.appointment.api.users.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        @DefaultValue({"http://localhost:3000", "http://localhost:5173", "http://localhost:4200"})
        List<String> allowedOrigins,
        @DefaultValue("PT1H")
        Duration maxAge) {
}
