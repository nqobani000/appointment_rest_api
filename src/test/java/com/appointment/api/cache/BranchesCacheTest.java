package com.appointment.api.cache;

import com.appointment.api.branches.dto.BranchServiceCriteria;
import com.appointment.api.branches.mapper.BranchMapper;
import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.branches.service.BranchService;
import com.appointment.api.branches.service.BranchServiceImpl;
import com.appointment.api.config.CacheConfig;
import com.appointment.api.exceptions.InvalidArgumentException;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = BranchesCacheTest.TestConfig.class, initializers = ConfigDataApplicationContextInitializer.class)
class BranchesCacheTest {

    static final AtomicLong NANOS = new AtomicLong();

    @Configuration
    @Import({CacheConfig.class, CacheKeySupport.class, BranchCacheKeys.class, CacheStartupCleaner.class,
            BranchServiceImpl.class})
    static class TestConfig {
        @Bean
        BranchRepository branchRepository() {
            return Mockito.mock(BranchRepository.class);
        }

        @Bean
        BranchMapper branchMapper() {
            return Mockito.mock(BranchMapper.class);
        }

        @Bean
        Ticker cacheTicker() {
            return NANOS::get;
        }
    }

    @Autowired
    BranchService service;
    @Autowired
    BranchRepository repository;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    CacheStartupCleaner cleaner;

    @BeforeEach
    void setUp() {
        Mockito.reset(repository);
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        NANOS.set(0);
        when(repository.findAllBranches(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    private void verifyQueries(int times) {
        verify(repository, times(times)).findAllBranches(any(), any(), any(), any());
    }

    @Test
    void listIsCachedAndSearchIsNormalized() {
        var page = PageRequest.of(0, 10);
        service.getAllBranches(new BranchServiceCriteria(" Nigel ", null, null), page);
        service.getAllBranches(new BranchServiceCriteria("nigel", null, null), page);
        verifyQueries(1);

        service.getAllBranches(new BranchServiceCriteria("nigel", null, null), PageRequest.of(1, 10));
        verifyQueries(2);
    }

    @Test
    void coordinateRequestsBypassCache() {
        var criteria = new BranchServiceCriteria("nigel", -26.2, 28.0);
        service.getAllBranches(criteria, PageRequest.of(0, 10));
        service.getAllBranches(criteria, PageRequest.of(0, 10));
        verifyQueries(2);
    }

    @Test
    void oversizedInputsBypassCache() {
        var longSearch = new BranchServiceCriteria("x".repeat(100), null, null);
        service.getAllBranches(longSearch, PageRequest.of(0, 101));
        service.getAllBranches(longSearch, PageRequest.of(0, 101));
        verifyQueries(2);
    }

    @Test
    void invalidCriteriaIsNotCached() {
        var invalid = new BranchServiceCriteria("a", 1.0, null);
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.getAllBranches(invalid, PageRequest.of(0, 10)))
                    .isInstanceOf(InvalidArgumentException.class);
        }
        verifyQueries(0);
    }

    @Test
    void entriesExpireAfter24Hours() {
        var criteria = new BranchServiceCriteria("a", null, null);
        var page = PageRequest.of(0, 10);
        service.getAllBranches(criteria, page);
        NANOS.set(Duration.ofHours(24).minusSeconds(1).toNanos());
        service.getAllBranches(criteria, page);
        verifyQueries(1);

        NANOS.set(Duration.ofHours(24).plusSeconds(1).toNanos());
        service.getAllBranches(criteria, page);
        verifyQueries(2);
    }

    @Test
    void startupClearsBranchesCache() {
        cacheManager.getCache(CacheNames.BRANCHES).put("k", "v");
        cleaner.clearAll();
        assertThat(cacheManager.getCache(CacheNames.BRANCHES).get("k")).isNull();
    }
}
