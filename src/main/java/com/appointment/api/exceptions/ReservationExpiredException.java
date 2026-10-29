package com.appointment.api.exceptions;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException() {
        super("The reservation has expired.");
    }
}
