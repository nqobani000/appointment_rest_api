package com.appointment.api.reservations.dao;

import com.appointment.api.branches.dao.Branch;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.customers.dao.Customer;
import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.reservations.ReservationStatus;
import com.appointment.api.services.dao.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_reservations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_id", nullable = false)
    private Consultant consultant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Setter(AccessLevel.NONE)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ReservationStatus status = ReservationStatus.HELD;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    /** Active only while HELD and strictly before expiresAt; no grace period. */
    public boolean isActiveAt(OffsetDateTime now) {
        return status == ReservationStatus.HELD && now.isBefore(expiresAt);
    }

    public boolean isStaleAt(OffsetDateTime now) {
        return status == ReservationStatus.HELD && !now.isBefore(expiresAt);
    }

    public void transitionTo(ReservationStatus next, OffsetDateTime now) {
        if (!status.canTransitionTo(next)) {
            throw new ConflictException("Reservation cannot change from " + status + " to " + next + ".");
        }
        status = next;
        updatedAt = now;
        if (next == ReservationStatus.CONFIRMED) {
            confirmedAt = now;
        } else if (next == ReservationStatus.RELEASED) {
            releasedAt = now;
        }
    }
}
