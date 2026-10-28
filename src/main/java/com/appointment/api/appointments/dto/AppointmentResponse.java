package com.appointment.api.appointments.dto;

import com.appointment.api.appointments.AppointmentStatus;
import com.appointment.api.commons.dto.Summaries;

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
