package com.appointment.api.cache;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    @NotNull
    private Duration defaultTtl;
    @Positive
    private long maximumSize;
    @Positive
    private int maxKeyLength;
    @Positive
    private int maxPageSize;
    @Positive
    private int maxPageNumber;
    private Map<String, Duration> ttl = new HashMap<>();

    public Duration ttlFor(String cacheName) {
        return ttl.getOrDefault(cacheName, defaultTtl);
    }
}
