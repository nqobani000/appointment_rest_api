package com.appointment.api.reservations.mapper;

import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.reservations.dto.ReservationResponse;
import com.appointment.api.commons.dto.Summaries.*;
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
}
