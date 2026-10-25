package com.appointment.api.services.dto;

import com.appointment.api.customers.CustomerType;

import java.util.Set;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String code,
        String name,
        String description,
        String category,
        Set<CustomerType> availableTo
) {
}