package com.appointment.api.appointments.service;

import com.appointment.api.appointments.dto.AppointmentResponse;
import com.appointment.api.appointments.mapper.AppointmentMapper;
import com.appointment.api.appointments.repository.AppointmentRepository;
import com.appointment.api.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointments;
    private final AppointmentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getByReferenceNo(String referenceNo) {
        String normalized = referenceNo.trim().toUpperCase(Locale.ROOT);
        return appointments.findByReferenceNo(normalized)
                .map(mapper::toResponse)
                .orElseThrow(() -> {
                    log.warn("Appointment lookup failed for reference {}", normalized);
                    return new ResourceNotFoundException("Appointment", normalized);
                });
    }
}
