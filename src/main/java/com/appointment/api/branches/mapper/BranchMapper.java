package com.appointment.api.branches.mapper;

import com.appointment.api.addresses.dao.Location;
import com.appointment.api.addresses.dto.LocationResponse;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.branches.dto.BranchResponse;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {

    public BranchResponse toResponse(Branch branch) {
        return toResponse(branch, null);
    }

    public BranchResponse toResponse(
            Branch branch,
            Double distanceKm
    ) {
        if (branch == null) {
            return null;
        }

        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .location(toLocationResponse(branch.getLocation()))
                .distanceKm(distanceKm)
                .build();
    }

    private LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }

        Point coordinates = location.getCoordinates();

        return new LocationResponse(
                location.getId(),
                location.getStreetNo(),
                location.getStreetName(),
                location.getSuburb(),
                location.getCity(),
                location.getProvince() != null
                        ? location.getProvince().getCode()
                        : null,
                location.getPostalCode(),
                location.getCountry(),
                coordinates != null ? coordinates.getY() : null,
                coordinates != null ? coordinates.getX() : null
        );
    }
}
