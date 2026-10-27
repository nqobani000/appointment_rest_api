package com.appointment.api.reservations.dto;

import java.util.UUID;

public final class Summaries {
    private Summaries() {
    }

    public record BranchSummary(UUID id, String name) {
    }

    public record ServiceSummary(UUID id, String name, int durationMinutes) {
    }

    public record ConsultantSummary(UUID id, String name, String surname) {
    }
}
