package com.appointment.api.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CacheKeySupport {

    private final CacheProperties properties;

    public String normalize(String search) {
        return search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    }

    public String pageKey(String normalizedSearch, Pageable pageable) {
        return normalizedSearch + "|" + pageable.getPageNumber() + "|" + pageable.getPageSize();
    }

    public boolean withinBounds(String normalizedSearch, Pageable pageable) {
        return pageable.isPaged()
                && normalizedSearch.length() <= properties.getMaxKeyLength()
                && pageable.getPageSize() <= properties.getMaxPageSize()
                && pageable.getPageNumber() <= properties.getMaxPageNumber();
    }
}
