package com.appointment.api.cache;

import com.appointment.api.services.dto.ServiceSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Key builder and bounding rules for the services caches; referenced from SpEL as @serviceCacheKeys. */
@Component("serviceCacheKeys")
@RequiredArgsConstructor
public class ServiceCacheKeys {

    private final CacheKeySupport support;

    public String listKey(ServiceSearchCriteria criteria, Pageable pageable) {
        return support.pageKey(support.normalize(search(criteria)), pageable) + "|" + pageable.getSort();
    }

    public String branchListKey(String branchId, ServiceSearchCriteria criteria, Pageable pageable) {
        return support.normalize(branchId) + "|" + listKey(criteria, pageable);
    }

    public boolean isCacheable(ServiceSearchCriteria criteria, Pageable pageable) {
        return support.withinBounds(support.normalize(search(criteria)), pageable);
    }

    private static String search(ServiceSearchCriteria criteria) {
        return criteria == null ? null : criteria.search();
    }
}
