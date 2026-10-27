package com.appointment.api.reservations.repository;

import com.appointment.api.reservations.dao.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select count(r) > 0 from Reservation r
            where r.consultant.id = :consultantId and r.status = com.appointment.api.reservations.ReservationStatus.HELD
              and r.expiresAt > :now and r.startTime < :end and r.endTime > :start
            """)
    boolean existsActiveOverlap(@Param("consultantId") UUID consultantId,
                                @Param("start") OffsetDateTime start,
                                @Param("end") OffsetDateTime end,
                                @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Reservation r set r.status = com.appointment.api.reservations.ReservationStatus.EXPIRED, r.updatedAt = :now
            where r.consultant.id = :consultantId and r.status = com.appointment.api.reservations.ReservationStatus.HELD
              and r.expiresAt <= :now
            """)
    int expireStaleForConsultant(@Param("consultantId") UUID consultantId, @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Reservation r set r.status = com.appointment.api.reservations.ReservationStatus.EXPIRED, r.updatedAt = :now
            where r.status = com.appointment.api.reservations.ReservationStatus.HELD and r.expiresAt <= :now
            """)
    int expireAllStale(@Param("now") OffsetDateTime now);
}
