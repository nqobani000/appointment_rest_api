package com.appointment.api.reservations.dto;

import com.appointment.api.appointments.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID appointmentId,
        String referenceNo,
        AppointmentStatus status,
        UUID reservationId,
        Summaries.BranchSummary branch,
        Summaries.ServiceSummary service,
        Summaries.ConsultantSummary consultant,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {
}
