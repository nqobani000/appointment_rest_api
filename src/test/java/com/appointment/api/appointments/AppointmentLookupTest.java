package com.appointment.api.appointments;

import com.appointment.api.appointments.controller.AppointmentController;
import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.appointments.dto.AppointmentResponse;
import com.appointment.api.appointments.mapper.AppointmentMapper;
import com.appointment.api.appointments.repository.AppointmentRepository;
import com.appointment.api.appointments.service.AppointmentServiceImpl;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.exceptions.GlobalExceptionHandler;
import com.appointment.api.exceptions.ResourceNotFoundException;
import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.services.dao.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentLookupTest {

    @Mock AppointmentRepository repository;
    AppointmentServiceImpl service;
    MockMvc mvc;
    Appointment appointment;

    @BeforeEach
    void setUp() throws Exception {
        service = new AppointmentServiceImpl(repository, new AppointmentMapper());
        mvc = MockMvcBuilders.standaloneSetup(new AppointmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        Branch branch = new Branch();
        branch.setId(UUID.randomUUID());
        branch.setName("Sandton Branch");
        Service svc = new Service();
        svc.setId(UUID.randomUUID());
        svc.setName("Home Loan Enquiry");
        svc.setDurationMinutes(30);
        Consultant consultant = new Consultant();
        consultant.setId(UUID.randomUUID());
        consultant.setName("Thandi");
        consultant.setSurname("Nkosi");
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .referenceNo("APT-20300101-3F9A1C7B2D")
                .branch(branch).service(svc).consultant(consultant).reservation(reservation)
                .startTime(OffsetDateTime.parse("2030-01-01T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2030-01-01T10:30:00Z"))
                .build();
    }

    @Test
    void returnsExistingAppointmentShape() throws Exception {
        when(repository.findByReferenceNo("APT-20300101-3F9A1C7B2D")).thenReturn(Optional.of(appointment));
        mvc.perform(get("/api/appointments/{ref}", "APT-20300101-3F9A1C7B2D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNo").value("APT-20300101-3F9A1C7B2D"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.branch.name").value("Sandton Branch"))
                .andExpect(jsonPath("$.service.durationMinutes").value(30))
                .andExpect(jsonPath("$.consultant.surname").value("Nkosi"))
                .andExpect(jsonPath("$.reservationId").value(appointment.getReservation().getId().toString()));
    }

    @Test
    void unknownReferenceReturns404() throws Exception {
        when(repository.findByReferenceNo("APT-NOPE")).thenReturn(Optional.empty());
        mvc.perform(get("/api/appointments/{ref}", "APT-NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void referenceIsTrimmedAndCaseInsensitive() {
        when(repository.findByReferenceNo("APT-20300101-3F9A1C7B2D")).thenReturn(Optional.of(appointment));
        AppointmentResponse response = service.getByReferenceNo(" apt-20300101-3f9a1c7b2d ");
        assertThat(response.appointmentId()).isEqualTo(appointment.getId());
        verify(repository).findByReferenceNo("APT-20300101-3F9A1C7B2D");
        assertThatThrownBy(() -> service.getByReferenceNo("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
