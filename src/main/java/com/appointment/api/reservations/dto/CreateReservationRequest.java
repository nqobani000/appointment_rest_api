package com.appointment.api.reservations.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull UUID branchId,
        @NotNull UUID serviceId,
        @NotNull OffsetDateTime startAt) {
}
