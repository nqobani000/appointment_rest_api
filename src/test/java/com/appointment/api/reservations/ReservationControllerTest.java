package com.appointment.api.reservations;

import com.appointment.api.exceptions.*;
import com.appointment.api.reservations.controller.ReservationController;
import com.appointment.api.reservations.dto.*;
import com.appointment.api.reservations.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock ReservationService service;
    MockMvc mvc;
    UUID id = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    Principal auth = new TestingAuthenticationToken(customerId.toString(), null);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ReservationController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void createReturns201AndValidatesBody() throws Exception {
        when(service.createReservation(eq(customerId), any())).thenReturn(new ReservationResponse(id, ReservationStatus.HELD, null, null, null,
                null, null, null, null, null, null, "tok"));
        String body = """
                {"branchId":"%s","serviceId":"%s","startAt":"2030-01-01T10:00:00Z"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        mvc.perform(post("/api/reservations").principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reservations").principal(auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errorsMapToStatusCodes() throws Exception {
        when(service.confirmReservation(any(), eq(customerId), any())).thenThrow(new ReservationExpiredException());
        mvc.perform(post("/api/reservations/{id}/confirm", id).principal(auth).header("X-Reservation-Token", "t"))
                .andExpect(status().isGone());
        doThrow(new ConflictException("x")).when(service).releaseReservation(any(), eq(customerId), any());
        mvc.perform(delete("/api/reservations/{id}", id).principal(auth)).andExpect(status().isConflict());
        when(service.getReservation(any(), eq(customerId), any())).thenThrow(new ResourceNotFoundException("Reservation", id));
        mvc.perform(get("/api/reservations/{id}", id).principal(auth)).andExpect(status().isNotFound());
    }

    @Test
    void releaseReturns204() throws Exception {
        mvc.perform(delete("/api/reservations/{id}", id).principal(auth).header("X-Reservation-Token", "t"))
                .andExpect(status().isNoContent());
    }
}
