package com.appointment.api.appointments.service;

import com.appointment.api.appointments.dto.AppointmentResponse;

public interface AppointmentService {

    AppointmentResponse getByReferenceNo(String referenceNo);
}
