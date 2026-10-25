package com.appointment.api.services.controller;

import com.appointment.api.services.dto.ServiceResponse;
import com.appointment.api.services.dto.ServiceResponsePage;
import com.appointment.api.services.dto.ServiceSearchCriteria;
import com.appointment.api.services.service.Services;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/services")
public class ServicesController {

    private final Services servicesService;

    @GetMapping
    public ResponseEntity<ServiceResponsePage> getAllActiveServices(
            @ModelAttribute ServiceSearchCriteria criteria, Pageable pageable) {
        return ResponseEntity.ok(servicesService.getAllActiveServices(criteria, pageable));
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponse> getActiveServiceById(
            @PathVariable UUID serviceId) {
        return ResponseEntity.ok(servicesService.getActiveServiceById(serviceId.toString()));
    }
}
