package com.appointment.api.appointments.controller;

import com.appointment.api.appointments.dto.AppointmentResponse;
import com.appointment.api.appointments.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/{referenceNo}")
    public ResponseEntity<AppointmentResponse> getByReferenceNo(@PathVariable String referenceNo) {
        return ResponseEntity.ok(appointmentService.getByReferenceNo(referenceNo));
    }
}
