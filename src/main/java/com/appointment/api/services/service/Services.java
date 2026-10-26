package com.appointment.api.services.service;

import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import org.springframework.data.domain.Pageable;

public interface Services {
    /**
     * Retrieves a paginated list of active services based on the provided criteria.
     *
     * @param criteria the criteria to filter services
     * @param pageable the pagination information
     * @return a paginated response of active services
     */
    ServiceResponsePage getAllActiveServices(ServiceSearchCriteria criteria, Pageable pageable);

    /**
     * Retrieves an active service by its ID.
     *
     * @param serviceId the ID of the service
     * @return the active service response
     */
    ServiceResponse getActiveServiceById(String serviceId);
}