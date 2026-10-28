package com.appointment.api.appointments.mapper;

import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.appointments.dto.AppointmentResponse;
import com.appointment.api.commons.dto.Summaries.BranchSummary;
import com.appointment.api.commons.dto.Summaries.ConsultantSummary;
import com.appointment.api.commons.dto.Summaries.ServiceSummary;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getReferenceNo(), a.getStatus(),
                a.getReservation() == null ? null : a.getReservation().getId(),
                new BranchSummary(a.getBranch().getId(), a.getBranch().getName()),
                new ServiceSummary(a.getService().getId(), a.getService().getName(), a.getService().getDurationMinutes()),
                new ConsultantSummary(a.getConsultant().getId(), a.getConsultant().getName(), a.getConsultant().getSurname()),
                a.getStartTime(), a.getEndTime());
    }
}
