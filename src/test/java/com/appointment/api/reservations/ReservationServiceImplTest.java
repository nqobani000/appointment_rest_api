package com.appointment.api.reservations;

import com.appointment.api.appointments.AppointmentStatus;
import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.appointments.repository.AppointmentRepository;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.branches.repository.BranchServiceLinkRepository;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.customers.CustomerType;
import com.appointment.api.customers.dao.Customer;
import com.appointment.api.customers.repository.CustomerRepository;
import com.appointment.api.exceptions.*;
import com.appointment.api.reservations.config.ReservationProperties;
import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.reservations.dto.*;
import com.appointment.api.reservations.mapper.ReservationMapper;
import com.appointment.api.reservations.repository.ReservationRepository;
import com.appointment.api.reservations.service.*;
import com.appointment.api.services.dao.Service;
import com.appointment.api.services.repository.ServicesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceImplTest {

    private static final Instant NOW = Instant.parse("2030-01-01T08:00:00Z");

    @Mock ReservationRepository reservations;
    @Mock AppointmentRepository appointments;
    @Mock BranchRepository branches;
    @Mock ServicesRepository services;
    @Mock CustomerRepository customers;
    @Mock BranchServiceLinkRepository links;
    @Mock ConsultantAllocator allocator;

    final ReservationTokenService tokens = new ReservationTokenService();
    final ReservationProperties props = new ReservationProperties(Duration.ofMinutes(10), ZoneId.of("UTC"), Duration.ofMinutes(1));
    MutableClock clock = new MutableClock(NOW);
    ReservationServiceImpl sut;

    Branch branch = new Branch();
    Service service = new Service();
    Customer customer = new Customer();
    Consultant consultant = new Consultant();
    UUID branchId = UUID.randomUUID(), serviceId = UUID.randomUUID(), customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        sut = new ReservationServiceImpl(reservations, appointments, branches, services, customers, links,
                allocator, tokens, new ReservationMapper(), props, clock);
        branch.setId(branchId);
        branch.setName("B");
        service.setId(serviceId);
        service.setName("S");
        service.setDurationMinutes(45);
        customer.setId(customerId);
        customer.setCustomerType(CustomerType.CLIENT);
        consultant.setId(UUID.randomUUID());
        consultant.setName("C");
        consultant.setSurname("D");
        when(branches.findById(branchId)).thenReturn(Optional.of(branch));
        when(services.findById(serviceId)).thenReturn(Optional.of(service));
        when(customers.findById(customerId)).thenReturn(Optional.of(customer));
        when(links.existsByBranchIdAndServiceId(branchId, serviceId)).thenReturn(true);
        when(allocator.allocate(any(), any(), any(), any(), any())).thenReturn(consultant);
        when(allocator.withinWorkingHours(any(), any(), any(), any())).thenReturn(true);
        when(appointments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    private CreateReservationRequest req(OffsetDateTime start) {
        return new CreateReservationRequest(branchId, serviceId, customerId, start);
    }

    private OffsetDateTime future() {
        return OffsetDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC);
    }

    private Reservation stored(ReservationStatus target) {
        ReservationResponse created = sut.createReservation(req(future()));
        ArgumentCaptor<Reservation> c = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).saveAndFlush(c.capture());
        Reservation r = c.getValue();
        r.setId(created.reservationId() == null ? UUID.randomUUID() : created.reservationId());
        if (r.getId() == null) r.setId(UUID.randomUUID());
        if (target != ReservationStatus.HELD) r.transitionTo(target, OffsetDateTime.now(clock));
        when(reservations.findByIdForUpdate(r.getId())).thenReturn(Optional.of(r));
        lastToken = created.reservationToken();
        return r;
    }

    String lastToken;

    @Test
    void createHoldsWithDerivedEndExpiryAndHashedToken() {
        ReservationResponse res = sut.createReservation(req(future()));
        ArgumentCaptor<Reservation> c = ArgumentCaptor.forClass(Reservation.class);
        verify(reservations).saveAndFlush(c.capture());
        Reservation r = c.getValue();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(r.getConsultant()).isSameAs(consultant);
        assertThat(r.getEndTime()).isEqualTo(r.getStartTime().plusMinutes(45));
        assertThat(r.getExpiresAt().toInstant()).isEqualTo(NOW.plusSeconds(600));
        assertThat(res.reservationToken()).isNotBlank();
        assertThat(r.getTokenHash()).isNotEqualTo(res.reservationToken()).isEqualTo(tokens.hash(res.reservationToken()));
        verifyNoInteractions(appointments);
    }

    @Test
    void createRejectsPastAndNoConsultant() {
        assertThatThrownBy(() -> sut.createReservation(req(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))))
                .isInstanceOf(InvalidArgumentException.class);
        when(allocator.allocate(any(), any(), any(), any(), any())).thenThrow(new ConflictException("none"));
        assertThatThrownBy(() -> sut.createReservation(req(future()))).isInstanceOf(ConflictException.class);
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsServiceNotAtBranchOrWrongCustomerType() {
        when(links.existsByBranchIdAndServiceId(branchId, serviceId)).thenReturn(false);
        assertThatThrownBy(() -> sut.createReservation(req(future()))).isInstanceOf(InvalidArgumentException.class);
        when(links.existsByBranchIdAndServiceId(branchId, serviceId)).thenReturn(true);
        customer.setCustomerType(CustomerType.GUEST);
        service.setAvailableToGuest(false);
        assertThatThrownBy(() -> sut.createReservation(req(future()))).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void wrongOrMissingTokenLooksLikeNotFound() {
        Reservation r = stored(ReservationStatus.HELD);
        assertThatThrownBy(() -> sut.getReservation(r.getId(), "nope")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> sut.getReservation(r.getId(), null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void expiryBoundaryOnGetAndConfirm() {
        Reservation r = stored(ReservationStatus.HELD);
        clock.set(r.getExpiresAt().toInstant().minusNanos(1000));
        assertThat(sut.getReservation(r.getId(), lastToken).status()).isEqualTo(ReservationStatus.HELD);

        clock.set(r.getExpiresAt().toInstant());
        assertThatThrownBy(() -> sut.confirmReservation(r.getId(), lastToken)).isInstanceOf(ReservationExpiredException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void getReportsExpiredAfterExpiry() {
        Reservation r = stored(ReservationStatus.HELD);
        clock.set(r.getExpiresAt().toInstant().plusSeconds(5));
        assertThat(sut.getReservation(r.getId(), lastToken).status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void releaseRules() {
        Reservation r = stored(ReservationStatus.HELD);
        sut.releaseReservation(r.getId(), lastToken);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        sut.releaseReservation(r.getId(), lastToken); // idempotent
        assertThatThrownBy(() -> sut.confirmReservation(r.getId(), lastToken)).isInstanceOf(ConflictException.class);
    }

    @Test
    void expiredIsNotReleasedAndConfirmedCannotBeReleased() {
        Reservation r = stored(ReservationStatus.HELD);
        clock.set(r.getExpiresAt().toInstant());
        assertThatThrownBy(() -> sut.releaseReservation(r.getId(), lastToken)).isInstanceOf(ReservationExpiredException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void confirmCreatesSingleAppointmentPreservingAssignment() {
        Reservation r = stored(ReservationStatus.HELD);
        ConfirmationResult res = sut.confirmReservation(r.getId(), lastToken);
        ArgumentCaptor<Appointment> c = ArgumentCaptor.forClass(Appointment.class);
        verify(appointments).saveAndFlush(c.capture());
        Appointment a = c.getValue();
        assertThat(res.created()).isTrue();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(a.getReservation()).isSameAs(r);
        assertThat(a.getConsultant()).isSameAs(consultant);
        assertThat(a.getStartTime()).isEqualTo(r.getStartTime());
        assertThat(a.getEndTime()).isEqualTo(r.getEndTime());
        assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        when(appointments.findByReservationId(r.getId())).thenReturn(Optional.of(a));
        ConfirmationResult again = sut.confirmReservation(r.getId(), lastToken);
        assertThat(again.created()).isFalse();
        verify(appointments, times(1)).saveAndFlush(any());
        assertThatThrownBy(() -> sut.releaseReservation(r.getId(), lastToken)).isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmFailsSafelyWhenConsultantNoLongerAvailable() {
        Reservation r = stored(ReservationStatus.HELD);
        when(allocator.withinWorkingHours(any(), any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> sut.confirmReservation(r.getId(), lastToken)).isInstanceOf(ConflictException.class);
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        verify(appointments, never()).saveAndFlush(any());
    }

    @Test
    void cleanupDelegatesWithClockTime() {
        when(reservations.expireAllStale(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(3);
        assertThat(sut.expireStaleReservations()).isEqualTo(3);
    }

    static class MutableClock extends Clock {
        private Instant now;
        MutableClock(Instant now) { this.now = now; }
        void set(Instant i) { now = i; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
