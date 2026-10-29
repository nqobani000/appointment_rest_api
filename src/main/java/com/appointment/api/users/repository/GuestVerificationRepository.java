package com.appointment.api.users.repository;

import com.appointment.api.users.dao.GuestVerification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuestVerificationRepository extends JpaRepository<GuestVerification, UUID> {

    Optional<GuestVerification> findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(String email);

    /** Locked so concurrent verify attempts cannot bypass the attempt limit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from GuestVerification v where v.email = :email and v.consumedAt is null order by v.createdAt desc limit 1")
    Optional<GuestVerification> lockLatestActive(@Param("email") String email);

    @Query("select v from GuestVerification v where v.email = :email and v.consumedAt is null")
    List<GuestVerification> findActive(@Param("email") String email);
}
