package com.appointment.api.reservations.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Housekeeping only; correctness never depends on this job running. */
@Component
@RequiredArgsConstructor
public class ReservationExpiryJob {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${app.reservation.cleanup-interval:PT1M}")
    public void expireStaleReservations() {
        reservationService.expireStaleReservations();
    }
}
