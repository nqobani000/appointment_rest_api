package com.appointment.api.branches.service;

import com.appointment.api.branches.dao.Branch;
import com.appointment.api.branches.dto.BranchResponsePage;
import com.appointment.api.branches.dto.BranchServiceCriteria;
import com.appointment.api.branches.mapper.BranchMapper;
import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import com.appointment.api.commons.geo.GeoDistance;
import com.appointment.api.exceptions.InvalidArgumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {
    static final int MAX_SEARCH_LENGTH = 100;

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Override
    @Cacheable(
            cacheNames = CacheNames.BRANCHES,
            key = "@branchCacheKeys.listKey(#criteria, #pageable)",
            condition = "@branchCacheKeys.isCacheable(#criteria, #pageable)"
    )
    @Transactional(readOnly = true)
    public BranchResponsePage getAllBranches(BranchServiceCriteria criteria, Pageable pageable) {
        validate(criteria);

        Double latitude = criteria == null ? null : criteria.latitude();
        Double longitude = criteria == null ? null : criteria.longitude();
        String search = normalizeSearch(criteria);

        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Branch> branches = branchRepository.findAllBranches(search, latitude, longitude, unsorted);

        boolean withDistance = latitude != null;
        return new BranchResponsePage(
                branches.getContent().stream()
                        .map(branch -> branchMapper.toResponse(
                                branch, withDistance ? distanceKm(branch, latitude, longitude) : null))
                        .toList(),
                branches.getNumber(),
                branches.getSize(),
                branches.getTotalElements(),
                branches.getTotalPages(),
                branches.isFirst(),
                branches.isLast()
        );
    }

    private static String normalizeSearch(BranchServiceCriteria criteria) {
        if (criteria == null || criteria.search() == null) return null;
        String search = criteria.search().trim();
        return search.isEmpty() ? null : escapeLike(search);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static void validate(BranchServiceCriteria criteria) {
        if (criteria == null) return;

        Double latitude = criteria.latitude();
        Double longitude = criteria.longitude();

        if ((latitude == null) != (longitude == null)) {
            log.error("Latitude and longitude must be provided together. Latitude: {}, Longitude: {}", latitude, longitude);
            throw new InvalidArgumentException("Latitude and longitude must be provided together.");
        }
        if (latitude != null && (!Double.isFinite(latitude) || latitude < -90 || latitude > 90)) {
            log.error("Latitude must be between -90 and 90. Provided: {}", latitude);
            throw new InvalidArgumentException("Latitude must be between -90 and 90.");
        }
        if (longitude != null && (!Double.isFinite(longitude) || longitude < -180 || longitude > 180)) {
            log.error("Longitude must be between -180 and 180. Provided: {}", longitude);
            throw new InvalidArgumentException("Longitude must be between -180 and 180.");
        }
        if (criteria.search() != null) {
            int searchLength = criteria.search().trim().length();
            if (searchLength > MAX_SEARCH_LENGTH) {
                log.error("Search must be at most {} characters. Provided: {}", MAX_SEARCH_LENGTH, searchLength);
                throw new InvalidArgumentException("Search must be at most " + MAX_SEARCH_LENGTH + " characters.");
            }
        }
    }

    private static Double distanceKm(Branch branch, double latitude, double longitude) {
        if (branch.getLocation() == null || branch.getLocation().getCoordinates() == null) return null;
        Point point = branch.getLocation().getCoordinates();
        double km = GeoDistance.haversineKm(latitude, longitude, point.getY(), point.getX());
        return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
