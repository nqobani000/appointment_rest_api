package com.appointment.api.services.dto;

import com.appointment.api.commons.dto.PageResponse;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.List;


@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceResponsePage extends PageResponse {

    private LinkedHashMap<String, List<ServiceResponse>> content;

    public ServiceResponsePage(LinkedHashMap<String, List<ServiceResponse>> content,
                               int page, int size, long totalElements, int totalPages,
                               boolean first, boolean last) {
        super(page, size, totalElements, totalPages, first, last);
        this.content = content;
    }
}