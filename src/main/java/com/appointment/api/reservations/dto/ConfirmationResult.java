package com.appointment.api.reservations.dto;

public record ConfirmationResult(AppointmentResponse appointment, boolean created) {
}
