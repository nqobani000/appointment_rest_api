package com.appointment.api.cache;

import com.appointment.api.branches.dto.BranchServiceCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** Key builder and bounding rules for the branches cache; referenced from SpEL as @branchCacheKeys. */
@Component("branchCacheKeys")
@RequiredArgsConstructor
public class BranchCacheKeys {

    private final CacheKeySupport support;

    // Client sort is ignored by the service (ordering is fixed by the query), so it is not part of the key.
    public String listKey(BranchServiceCriteria criteria, Pageable pageable) {
        return support.pageKey(support.normalize(search(criteria)), pageable);
    }

    public boolean isCacheable(BranchServiceCriteria criteria, Pageable pageable) {
        boolean hasCoordinates = criteria != null && (criteria.latitude() != null || criteria.longitude() != null);
        return !hasCoordinates && support.withinBounds(support.normalize(search(criteria)), pageable);
    }

    private static String search(BranchServiceCriteria criteria) {
        return criteria == null ? null : criteria.search();
    }
}
