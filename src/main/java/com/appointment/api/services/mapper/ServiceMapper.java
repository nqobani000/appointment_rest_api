
package com.appointment.api.services.mapper;

import com.appointment.api.users.CustomerType;
import com.appointment.api.services.dao.Service;
import com.appointment.api.services.dto.ServiceResponse;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
public class ServiceMapper {

    public ServiceResponse toResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getDescription(),
                service.getCategory().getName(),
                getAvailableTo(service)
        );
    }

    private Set<CustomerType> getAvailableTo(Service service) {
        Set<CustomerType> availableTo = EnumSet.noneOf(CustomerType.class);

        if (service.isAvailableToRegistered()) {
            availableTo.add(CustomerType.CLIENT);
        }

        if (service.isAvailableToGuest()) {
            availableTo.add(CustomerType.GUEST);
        }

        return Set.copyOf(availableTo);
    }
}
