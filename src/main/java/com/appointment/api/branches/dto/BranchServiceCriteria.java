package com.appointment.api.branches.dto;

public record BranchServiceCriteria(
        String search,
        Double latitude,
        Double longitude
) {
}
