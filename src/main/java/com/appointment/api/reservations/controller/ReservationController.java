package com.appointment.api.reservations.controller;

import com.appointment.api.reservations.dto.*;
import com.appointment.api.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    static final String TOKEN_HEADER = "X-Reservation-Token";

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID reservationId,
                                                              @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId, token));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> release(@PathVariable UUID reservationId,
                                        @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        reservationService.releaseReservation(reservationId, token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(@PathVariable UUID reservationId,
                                                       @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        ConfirmationResult result = reservationService.confirmReservation(reservationId, token);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.appointment());
    }
}
