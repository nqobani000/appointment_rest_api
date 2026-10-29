package com.appointment.api.reservations.controller;

import com.appointment.api.appointments.dto.AppointmentResponse;
import com.appointment.api.reservations.dto.*;
import com.appointment.api.reservations.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    static final String TOKEN_HEADER = "X-Reservation-Token";

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponse> create(Principal principal,
                                                      @Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(customerId(principal), request));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getReservation(Principal principal,
                                                              @PathVariable UUID reservationId,
                                                              @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        return ResponseEntity.ok(reservationService.getReservation(reservationId, customerId(principal), token));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> release(Principal principal,
                                        @PathVariable UUID reservationId,
                                        @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        reservationService.releaseReservation(reservationId, customerId(principal), token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(Principal principal,
                                                       @PathVariable UUID reservationId,
                                                       @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
        ConfirmationResult result = reservationService.confirmReservation(reservationId, customerId(principal), token);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.appointment());
    }

    /** The JWT subject is the authenticated customer's id. */
    private UUID customerId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
