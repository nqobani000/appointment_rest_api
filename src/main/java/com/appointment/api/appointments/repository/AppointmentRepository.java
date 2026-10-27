package com.appointment.api.appointments.repository;

import com.appointment.api.appointments.dao.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByReservationId(UUID reservationId);

    /** Only CONFIRMED appointments consume capacity; overlap uses the half-open interval [start, end). */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.consultant.id = :consultantId and a.status = com.appointment.api.appointments.AppointmentStatus.CONFIRMED
              and a.startTime < :end and a.endTime > :start
            """)
    boolean existsOverlap(@Param("consultantId") UUID consultantId,
                          @Param("start") OffsetDateTime start,
                          @Param("end") OffsetDateTime end);
}
