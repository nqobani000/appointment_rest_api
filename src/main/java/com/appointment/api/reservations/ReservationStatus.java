package com.appointment.api.reservations;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    HELD,
    CONFIRMED,
    RELEASED,
    EXPIRED;

    public Set<ReservationStatus> allowedNext() {
        return this == HELD ? EnumSet.of(CONFIRMED, RELEASED, EXPIRED) : EnumSet.noneOf(ReservationStatus.class);
    }

    public boolean canTransitionTo(ReservationStatus next) {
        return allowedNext().contains(next);
    }
}
