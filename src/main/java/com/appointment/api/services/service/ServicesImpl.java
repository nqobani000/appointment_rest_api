package com.appointment.api.services.service;

import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.cache.CacheNames;
import org.springframework.cache.annotation.Cacheable;
import com.appointment.api.exceptions.InvalidArgumentException;
import com.appointment.api.exceptions.ResourceNotFoundException;
import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import com.appointment.api.services.mapper.ServiceMapper;
import com.appointment.api.services.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicesImpl implements Services {

    private final ServicesRepository servicesRepository;
    private final ServiceMapper serviceMapper;
    private final BranchRepository branchRepository;


    @Override
    @Cacheable(
            cacheNames = CacheNames.SERVICES,
            key = "@serviceCacheKeys.listKey(#criteria, #pageable)",
            condition = "@serviceCacheKeys.isCacheable(#criteria, #pageable)"
    )
    public ServiceResponsePage getAllActiveServices(ServiceSearchCriteria criteria, Pageable pageable) {

        String filter = getFilter(criteria);

        Page<com.appointment.api.services.dao.Service> activeServices = servicesRepository.findActiveServices(filter, pageable);

        return toPage(activeServices);
    }

    @Override
    @Cacheable(
            cacheNames = CacheNames.BRANCH_SERVICES,
            key = "@serviceCacheKeys.branchListKey(#branchId, #criteria, #pageable)",
            condition = "@serviceCacheKeys.isCacheable(#criteria, #pageable)"
    )
    public ServiceResponsePage getAllActiveServicesByBranchId(String branchId, ServiceSearchCriteria criteria, Pageable pageable) {
        UUID id = resolveUUID(branchId, "branch");
        if (!branchRepository.existsById(id)) {
            throw new ResourceNotFoundException("Branch", id);
        }
        return toPage(servicesRepository.findActiveServicesByBranchId(id, getFilter(criteria), pageable));
    }

    private ServiceResponsePage toPage(Page<com.appointment.api.services.dao.Service> page) {
        LinkedHashMap<String, List<ServiceResponse>> groupedServices = page
                .getContent()
                .stream()
                .map(serviceMapper::toResponse)
                .collect(Collectors.groupingBy(
                        ServiceResponse::category,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new ServiceResponsePage(
                groupedServices,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @Override
    @Cacheable(
            cacheNames = CacheNames.SERVICE_BY_ID,
            key = "#serviceId"
    )
    public ServiceResponse getActiveServiceById(String serviceId) {
        UUID id = resolveUUID(serviceId, "service");
        return servicesRepository.findActiveServiceById(id)
                .map(serviceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Active service", id));
    }

    private static @NonNull UUID resolveUUID(String value, String label) {
        UUID id;
        try {
            id = UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidArgumentException("Invalid " + label + " ID: " + value, exception);
        }
        return id;
    }

    private static @NonNull String getFilter(ServiceSearchCriteria criteria) {
        if (criteria == null || criteria.search() == null) return "";
        return criteria.search().trim();
    }
}