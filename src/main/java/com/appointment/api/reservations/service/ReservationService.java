package com.appointment.api.reservations.service;

import com.appointment.api.reservations.dto.ConfirmationResult;
import com.appointment.api.reservations.dto.CreateReservationRequest;
import com.appointment.api.reservations.dto.ReservationResponse;

import java.util.UUID;

public interface ReservationService {

    /**
     * Creates a new reservation based on the provided request.
     * @param request the request containing reservation details
     * @return the response containing the created reservation details
     */
    ReservationResponse createReservation(CreateReservationRequest request);

    /**
     * Retrieves a reservation by its ID and validates the provided token.
     * @param reservationId the ID of the reservation
     * @param token the token for authorization (optional)
     * @return the response containing the reservation details
     */
    ReservationResponse getReservation(UUID reservationId, String token);

    /**
     * Releases a reservation by its ID and validates the provided token.
     * @param reservationId the ID of the reservation
     * @param token the token for authorization (optional)
     */
    void releaseReservation(UUID reservationId, String token);

    /**
     * Confirms a reservation by its ID and validates the provided token.
     * @param reservationId the ID of the reservation
     * @param token the token for authorization (optional)
     * @return the result of the confirmation
     */
    ConfirmationResult confirmReservation(UUID reservationId, String token);

    /**
     * Expires all stale reservations.
     * @return the number of reservations that were expired
     */
    int expireStaleReservations();
}
