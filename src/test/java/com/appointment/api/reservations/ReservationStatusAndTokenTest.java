package com.appointment.api.reservations;

import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.reservations.service.ReservationTokenService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStatusAndTokenTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2030, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    private Reservation held() {
        Reservation r = new Reservation();
        r.setExpiresAt(T0);
        return r;
    }

    @Test
    void onlyHeldHasTransitions() {
        assertThat(ReservationStatus.HELD.allowedNext())
                .containsExactlyInAnyOrder(ReservationStatus.CONFIRMED, ReservationStatus.RELEASED, ReservationStatus.EXPIRED);
        for (ReservationStatus s : new ReservationStatus[]{ReservationStatus.CONFIRMED, ReservationStatus.RELEASED, ReservationStatus.EXPIRED}) {
            assertThat(s.allowedNext()).isEmpty();
        }
    }

    @Test
    void terminalStatesRejectTransitions() {
        Reservation r = held();
        r.transitionTo(ReservationStatus.RELEASED, T0);
        assertThat(r.getReleasedAt()).isEqualTo(T0);
        assertThatThrownBy(() -> r.transitionTo(ReservationStatus.CONFIRMED, T0)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> r.transitionTo(ReservationStatus.HELD, T0)).isInstanceOf(ConflictException.class);
    }

    @Test
    void expiryBoundaryIsStrict() {
        Reservation r = held();
        assertThat(r.isActiveAt(T0.minusNanos(1))).isTrue();
        assertThat(r.isActiveAt(T0)).isFalse();
        assertThat(r.isStaleAt(T0)).isTrue();
        assertThat(r.isActiveAt(T0.plusNanos(1))).isFalse();
    }

    @Test
    void tokenIsRandomHashedAndVerified() {
        ReservationTokenService svc = new ReservationTokenService();
        String a = svc.generate();
        assertThat(a).isNotEqualTo(svc.generate());
        assertThat(a.length()).isGreaterThanOrEqualTo(43); // 256 bits base64url
        String hash = svc.hash(a);
        assertThat(hash).isNotEqualTo(a).hasSize(64);
        assertThat(svc.matches(a, hash)).isTrue();
        assertThat(svc.matches(svc.generate(), hash)).isFalse();
        assertThat(svc.matches(null, hash)).isFalse();
    }
}
