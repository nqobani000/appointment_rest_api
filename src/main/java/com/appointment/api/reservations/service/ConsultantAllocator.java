package com.appointment.api.reservations.service;

import com.appointment.api.appointments.repository.AppointmentRepository;
import com.appointment.api.consultants.ConsultantStatus;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.consultants.repository.ConsultantRepository;
import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.reservations.config.ReservationProperties;
import com.appointment.api.reservations.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Single source of truth for consultant availability: working hours, confirmed appointments
 * and unexpired holds over the half-open interval [start, end).
 */
@Component
@RequiredArgsConstructor
public class ConsultantAllocator {

    private final ConsultantRepository consultantRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationProperties properties;

    @Transactional(propagation = Propagation.MANDATORY)
    public Consultant allocate(UUID branchId, UUID serviceId, OffsetDateTime start, OffsetDateTime end,
                               OffsetDateTime now) {
        for (UUID candidateId : consultantRepository.findEligibleIds(ConsultantStatus.ACTIVE, branchId, serviceId)) {
            if (!withinWorkingHours(candidateId, branchId, start, end)) {
                continue;
            }

            Consultant locked = consultantRepository.findByIdForUpdate(candidateId).orElse(null);
            if (locked != null
                    && locked.getStatus() == ConsultantStatus.ACTIVE
                    && isFreeWhileLocked(candidateId, start, end, now)) {
                return locked;
            }
        }
        throw new ConflictException("No consultant is available for the requested time.");
    }

    /**
     * Caller must hold the consultant lock; stale holds are expired first so the DB constraint stays accurate.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean isFreeWhileLocked(UUID consultantId, OffsetDateTime start, OffsetDateTime end, OffsetDateTime now) {
        reservationRepository.expireStaleForConsultant(consultantId, now);
        return !appointmentRepository.existsOverlap(consultantId, start, end)
                && !reservationRepository.existsActiveOverlap(consultantId, start, end, now);
    }

    public boolean withinWorkingHours(UUID consultantId, UUID branchId, OffsetDateTime start, OffsetDateTime end) {
        ZonedDateTime s = start.atZoneSameInstant(properties.zone());
        ZonedDateTime e = end.atZoneSameInstant(properties.zone());
        if (!s.toLocalDate().equals(e.toLocalDate())) {
            return false;
        }
        return consultantRepository.hasWorkingWindow(
                consultantId, branchId,
                (short) s.getDayOfWeek().getValue(), s.toLocalTime(), e.toLocalTime());
    }
}