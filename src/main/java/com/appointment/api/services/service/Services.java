package com.appointment.api.services.service;

import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import org.springframework.data.domain.Pageable;

public interface Services {
    ServiceResponsePage getAllActiveServices(ServiceSearchCriteria criteria, Pageable pageable);

    ServiceResponse getActiveServiceById(String serviceId);
}
