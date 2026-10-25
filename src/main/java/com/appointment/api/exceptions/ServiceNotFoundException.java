package com.appointment.api.exceptions;

import java.util.UUID;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(UUID serviceId) {
        super("Active service not found: " + serviceId);
    }
}