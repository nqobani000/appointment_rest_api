package com.appointment.api.consultants.repository;

import com.appointment.api.consultants.ConsultantStatus;
import com.appointment.api.consultants.dao.Consultant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultantRepository extends JpaRepository<Consultant, UUID> {

    @Query("""
            select c.id from Consultant c
            where c.status = :status
              and exists (select 1 from ConsultantBranch cb where cb.consultant = c and cb.branch.id = :branchId)
              and exists (select 1 from ConsultantService cs where cs.consultant = c and cs.service.id = :serviceId)
            order by c.id
            """)
    List<UUID> findEligibleIds(@Param("status") ConsultantStatus status,
                               @Param("branchId") UUID branchId,
                               @Param("serviceId") UUID serviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consultant c where c.id = :id")
    Optional<Consultant> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select count(a) > 0 from ConsultantAvailability a
            where a.consultant.id = :consultantId and a.branch.id = :branchId and a.dayOfWeek = :dow
              and a.startTime <= :start and a.endTime >= :end
            """)
    boolean hasWorkingWindow(@Param("consultantId") UUID consultantId, @Param("branchId") UUID branchId,
                             @Param("dow") Short dayOfWeek,
                             @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
}
