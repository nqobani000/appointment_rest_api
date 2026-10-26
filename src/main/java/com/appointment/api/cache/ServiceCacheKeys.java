package com.appointment.api.cache;

import com.appointment.api.services.dto.ServiceSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Key builder and bounding rules for the services caches; referenced from SpEL as @serviceCacheKeys. */
@Component("serviceCacheKeys")
@RequiredArgsConstructor
public class ServiceCacheKeys {

    private final CacheProperties properties;

    public String listKey(ServiceSearchCriteria criteria, Pageable pageable) {
        return normalizedFilter(criteria) + "|" + pageable.getPageNumber() + "|"
                + pageable.getPageSize() + "|" + pageable.getSort();
    }

    public boolean isCacheable(ServiceSearchCriteria criteria, Pageable pageable) {
        return pageable.isPaged()
                && normalizedFilter(criteria).length() <= properties.getMaxKeyLength()
                && pageable.getPageSize() <= properties.getMaxPageSize()
                && pageable.getPageNumber() <= properties.getMaxPageNumber();
    }
/**/
    private static String normalizedFilter(ServiceSearchCriteria criteria) {
        if (criteria == null || criteria.search() == null) return "";
        return criteria.search().trim().toLowerCase(Locale.ROOT);
    }
}
