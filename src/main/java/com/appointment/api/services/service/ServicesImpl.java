package com.appointment.api.services.service;

import com.appointment.api.exceptions.InvalidArgumentException;
import com.appointment.api.exceptions.ServiceNotFoundException;
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


    @Override
    public ServiceResponsePage getAllActiveServices(ServiceSearchCriteria criteria, Pageable pageable) {

        String filter = getFilter(criteria);

        Page<com.appointment.api.services.dao.Service> activeServices = servicesRepository.findActiveServices(filter, pageable);

        LinkedHashMap<String, List<ServiceResponse>> groupedServices = activeServices
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
                activeServices.getNumber(),
                activeServices.getSize(),
                activeServices.getTotalElements(),
                activeServices.getTotalPages(),
                activeServices.isFirst(),
                activeServices.isLast()
        );
    }

    @Override
    public ServiceResponse getActiveServiceById(String serviceId) {
        UUID id = resolveUUID(serviceId);
        return servicesRepository.findActiveServiceById(id)
                .map(serviceMapper::toResponse)
                .orElseThrow(() -> new ServiceNotFoundException(id));
    }

    private static @NonNull UUID resolveUUID(String serviceId) {
        UUID id;
        try {
            id = UUID.fromString(serviceId);
        } catch (IllegalArgumentException exception) {
            throw new InvalidArgumentException("Invalid service ID: " + serviceId, exception);
        }
        return id;
    }

    private static @NonNull String getFilter(ServiceSearchCriteria criteria) {
        if (criteria == null || criteria.search() == null) return "";
        return criteria.search().trim();
    }
}