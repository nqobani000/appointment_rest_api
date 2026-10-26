package com.appointment.api.branches.dto;

import com.appointment.api.addresses.dto.LocationResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private UUID id;
    private String name;
    private LocationResponse location;
    private Double distanceKm;
}
