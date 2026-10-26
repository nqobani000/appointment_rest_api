package com.appointment.api.cache;

import com.appointment.api.config.CacheConfig;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import com.appointment.api.services.mapper.ServiceMapper;
import com.appointment.api.services.repository.ServicesRepository;
import com.appointment.api.services.service.Services;
import com.appointment.api.services.service.ServicesImpl;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = ServicesCacheTest.TestConfig.class, initializers = ConfigDataApplicationContextInitializer.class)
class ServicesCacheTest {

    static final AtomicLong NANOS = new AtomicLong();

    @Configuration
    @Import({CacheConfig.class, CacheKeySupport.class, ServiceCacheKeys.class, CacheStartupCleaner.class, ServicesImpl.class})
    static class TestConfig {
        @Bean
        ServicesRepository servicesRepository() {
            return Mockito.mock(ServicesRepository.class);
        }

        @Bean
        ServiceMapper serviceMapper() {
            return Mockito.mock(ServiceMapper.class);
        }

        @Bean
        Ticker cacheTicker() {
            return NANOS::get;
        }
    }

    @Autowired
    Services services;
    @Autowired
    ServicesRepository repository;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    CacheStartupCleaner cleaner;

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        NANOS.set(0);
        when(repository.findActiveServices(anyString(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void listIsCachedAndKeyIsNormalized() {
        var page = PageRequest.of(0, 10);
        services.getAllActiveServices(new ServiceSearchCriteria(" Loan "), page);
        services.getAllActiveServices(new ServiceSearchCriteria("loan"), page);
        verify(repository, times(1)).findActiveServices(anyString(), any());

        services.getAllActiveServices(new ServiceSearchCriteria("loan"), PageRequest.of(1, 10));
        verify(repository, times(2)).findActiveServices(anyString(), any());
    }

    @Test
    void overLongFilterBypassesCache() {
        var criteria = new ServiceSearchCriteria("x".repeat(101));
        services.getAllActiveServices(criteria, PageRequest.of(0, 10));
        services.getAllActiveServices(criteria, PageRequest.of(0, 10));
        verify(repository, times(2)).findActiveServices(anyString(), any());
    }

    @Test
    void oversizedPageBypassesCache() {
        var criteria = new ServiceSearchCriteria("a");
        services.getAllActiveServices(criteria, PageRequest.of(0, 101));
        services.getAllActiveServices(criteria, PageRequest.of(0, 101));
        verify(repository, times(2)).findActiveServices(anyString(), any());
    }

    @Test
    void notFoundIsNotCached() {
        UUID id = UUID.randomUUID();
        when(repository.findActiveServiceById(id)).thenReturn(Optional.empty());
        for (int i = 0; i < 2; i++) {
            try {
                services.getActiveServiceById(id.toString());
            } catch (RuntimeException ignored) {
            }
        }
        verify(repository, times(2)).findActiveServiceById(id);
    }

    @Test
    void entriesExpireAfter24Hours() {
        var page = PageRequest.of(0, 10);
        var criteria = new ServiceSearchCriteria("a");
        services.getAllActiveServices(criteria, page);
        NANOS.set(Duration.ofHours(24).minusSeconds(1).toNanos());
        services.getAllActiveServices(criteria, page);
        verify(repository, times(1)).findActiveServices(anyString(), any());

        NANOS.set(Duration.ofHours(24).plusSeconds(1).toNanos());
        services.getAllActiveServices(criteria, page);
        verify(repository, times(2)).findActiveServices(anyString(), any());
    }

    @Test
    void startupClearsAllCaches() {
        cacheManager.getCache(CacheNames.SERVICES).put("k", "v");
        cleaner.clearAll();
        assertThat(cacheManager.getCache(CacheNames.SERVICES).get("k")).isNull();
    }
}
