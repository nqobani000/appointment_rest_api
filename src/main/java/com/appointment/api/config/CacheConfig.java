package com.appointment.api.config;

import com.appointment.api.cache.CacheNames;
import com.appointment.api.cache.CacheProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    private static final List<String> CACHE_NAMES = Stream.of(
            CacheNames.SERVICES,
            CacheNames.SERVICE_BY_ID,
            CacheNames.BRANCHES,
            CacheNames.BRANCH_SERVICES
    ).toList();

    @Bean
    public Ticker cacheTicker() {
        return Ticker.systemTicker();
    }

    @Bean
    public CacheManager cacheManager(CacheProperties properties, Ticker cacheTicker) {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(CACHE_NAMES.stream()
                .map(name -> (org.springframework.cache.Cache) new CaffeineCache(name,
                        Caffeine.newBuilder()
                                .ticker(cacheTicker)
                                .expireAfterWrite(properties.ttlFor(name))
                                .maximumSize(properties.getMaximumSize())
                                .build()))
                .toList());
        return manager;
    }
}
