package com.appointment.api.reservations.mapper;

import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.reservations.dto.AppointmentResponse;
import com.appointment.api.reservations.dto.ReservationResponse;
import com.appointment.api.reservations.dto.Summaries.*;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation r, String token) {
        return new ReservationResponse(r.getId(), r.getStatus(),
                r.getBranch().getId(), r.getService().getId(), r.getConsultant().getId(),
                new BranchSummary(r.getBranch().getId(), r.getBranch().getName()),
                new ServiceSummary(r.getService().getId(), r.getService().getName(), r.getService().getDurationMinutes()),
                new ConsultantSummary(r.getConsultant().getId(), r.getConsultant().getName(), r.getConsultant().getSurname()),
                r.getStartTime(), r.getEndTime(), r.getExpiresAt(), token);
    }

    public AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getReferenceNo(), a.getStatus(),
                a.getReservation() == null ? null : a.getReservation().getId(),
                new BranchSummary(a.getBranch().getId(), a.getBranch().getName()),
                new ServiceSummary(a.getService().getId(), a.getService().getName(), a.getService().getDurationMinutes()),
                new ConsultantSummary(a.getConsultant().getId(), a.getConsultant().getName(), a.getConsultant().getSurname()),
                a.getStartTime(), a.getEndTime());
    }
}
