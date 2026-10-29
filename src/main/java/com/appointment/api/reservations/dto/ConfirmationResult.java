package com.appointment.api.reservations.dto;

import com.appointment.api.appointments.dto.AppointmentResponse;

public record ConfirmationResult(AppointmentResponse appointment, boolean created) {
}
