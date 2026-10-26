package com.appointment.api.addresses.dto;

import java.util.UUID;

public record LocationResponse(
        UUID id,
        String streetNo,
        String streetName,
        String suburb,
        String city,
        String province,
        String postalCode,
        String country,
        Double latitude,
        Double longitude
) {
}