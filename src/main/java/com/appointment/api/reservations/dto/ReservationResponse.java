package com.appointment.api.reservations.dto;

import com.appointment.api.commons.dto.Summaries;
import com.appointment.api.reservations.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

/** reservationToken is populated only in the creation response. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationResponse(
        UUID reservationId,
        ReservationStatus status,
        UUID branchId,
        UUID serviceId,
        UUID consultantId,
        Summaries.BranchSummary branch,
        Summaries.ServiceSummary service,
        Summaries.ConsultantSummary consultant,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime expiresAt,
        String reservationToken) {
}
