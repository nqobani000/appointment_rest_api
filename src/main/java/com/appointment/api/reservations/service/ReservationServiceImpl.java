package com.appointment.api.reservations.service;

import com.appointment.api.appointments.AppointmentStatus;
import com.appointment.api.appointments.dao.Appointment;
import com.appointment.api.appointments.repository.AppointmentRepository;
import com.appointment.api.branches.dao.Branch;
import com.appointment.api.branches.repository.BranchRepository;
import com.appointment.api.branches.repository.BranchServiceLinkRepository;
import com.appointment.api.consultants.ConsultantStatus;
import com.appointment.api.consultants.dao.Consultant;
import com.appointment.api.customers.CustomerType;
import com.appointment.api.customers.dao.Customer;
import com.appointment.api.customers.repository.CustomerRepository;
import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.exceptions.InvalidArgumentException;
import com.appointment.api.exceptions.ReservationExpiredException;
import com.appointment.api.exceptions.ResourceNotFoundException;
import com.appointment.api.reservations.ReservationStatus;
import com.appointment.api.reservations.config.ReservationProperties;
import com.appointment.api.reservations.dao.Reservation;
import com.appointment.api.reservations.dto.ConfirmationResult;
import com.appointment.api.reservations.dto.CreateReservationRequest;
import com.appointment.api.reservations.dto.ReservationResponse;
import com.appointment.api.reservations.mapper.ReservationMapper;
import com.appointment.api.reservations.repository.ReservationRepository;
import com.appointment.api.services.dao.Service;
import com.appointment.api.services.repository.ServicesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final String RESERVATION = "Reservation";

    private final ReservationRepository reservationRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;
    private final ServicesRepository servicesRepository;
    private final CustomerRepository customerRepository;
    private final BranchServiceLinkRepository branchServiceLinkRepository;
    private final ConsultantAllocator allocator;
    private final ReservationTokenService tokenService;
    private final ReservationMapper mapper;
    private final ReservationProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime start = request.startAt();

        if (!start.isAfter(now)) {
            log.error("Attempted to create a reservation in the past: now={}, start={}", now, start);
            throw new InvalidArgumentException("The requested appointment time must be in the future.");
        }

        Branch branch = getBranch(request);
        Service service = getService(request);
        Customer customer = getCustomer(request);

        validateServiceEligibility(branch, service, customer);

        OffsetDateTime end = start.plusMinutes(service.getDurationMinutes());
        Consultant consultant = allocator.allocate(branch.getId(), service.getId(), start, end, now);

        log.info("Allocated consultant {} for reservation request: branch={}, service={}, customer={}, start={}, end={}",
                consultant.getId(), branch.getId(), service.getId(), customer.getId(), start, end);

        String token = tokenService.generate();

        Reservation reservation = Reservation.builder()
                .branch(branch)
                .service(service)
                .consultant(consultant)
                .customer(customer)
                .startTime(start)
                .endTime(end)
                .tokenHash(tokenService.hash(token))
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(now.plus(properties.holdDuration()))
                .build();

        reservationRepository.saveAndFlush(reservation);
        log.info("Created reservation {} for customer {} with consultant {} at branch {} for service {} from {} to {}",
                reservation.getId(), customer.getId(), consultant.getId(), branch.getId(), service.getId(),
                start, end);

        return mapper.toResponse(reservation, token);
    }

    @Override
    @Transactional
    public ReservationResponse getReservation(UUID reservationId, String token) {
        Reservation reservation = loadAuthorized(reservationId, token);
        expireIfStale(reservation, OffsetDateTime.now(clock));
        return mapper.toResponse(reservation, null);
    }

    @Override
    @Transactional(noRollbackFor = ReservationExpiredException.class)
    public void releaseReservation(UUID reservationId, String token) {
        log.info("Releasing reservation {} with token {}", reservationId, token);
        OffsetDateTime now = OffsetDateTime.now(clock);
        Reservation reservation = loadAuthorized(reservationId, token);
        expireIfStale(reservation, now);
        switch (reservation.getStatus()) {
            case HELD -> reservation.transitionTo(ReservationStatus.RELEASED, now);
            case RELEASED -> log.info("Reservation {} has been released.", reservationId);
            case EXPIRED -> {
                log.warn("Attempted to release an expired reservation {}.", reservationId);
                throw new ReservationExpiredException();
            }
            case CONFIRMED -> {
                log.error("Attempted to release a confirmed reservation {}.", reservationId);
                throw new ConflictException("The reservation has already been confirmed.");
            }
        }
    }

    @Override
    @Transactional(noRollbackFor = {ReservationExpiredException.class, ConflictException.class})
    public ConfirmationResult confirmReservation(UUID reservationId, String token) {
        log.info("Confirming reservation {}", reservationId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        Reservation reservation = loadAuthorized(reservationId, token);
        expireIfStale(reservation, now);

        switch (reservation.getStatus()) {
            case CONFIRMED -> {
                Appointment existing = appointmentRepository.findByReservationId(reservationId)
                        .orElseThrow(() -> new ConflictException("The reservation is in an inconsistent state."));
                log.info("Reservation {} has already been confirmed with appointment {}.", reservationId, existing.getId());
                return new ConfirmationResult(mapper.toResponse(existing), false);
            }
            case RELEASED -> {
                log.warn("Attempted to confirm a released reservation {}.", reservationId);
                throw new ConflictException("The reservation has been released.");
            }
            case EXPIRED -> {
                log.warn("Attempted to confirm an expired reservation {}.", reservationId);
                throw new ReservationExpiredException();
            }
            case HELD -> log.info("Reservation {} is held, attempting confirmation.", reservationId);
        }

        Consultant consultant = reservation.getConsultant();
        boolean stillValid = consultant.getStatus() == ConsultantStatus.ACTIVE
                && allocator.withinWorkingHours(consultant.getId(), reservation.getBranch().getId(),
                reservation.getStartTime(), reservation.getEndTime())
                && !appointmentRepository.existsOverlap(consultant.getId(),
                reservation.getStartTime(), reservation.getEndTime());
        if (!stillValid) {
            reservation.transitionTo(ReservationStatus.RELEASED, now);
            log.error("The assigned consultant {} is no longer available for reservation {}.", consultant.getId(), reservationId);
            throw new ConflictException("The assigned consultant is no longer available. Please book again.");
        }

        Appointment appointment = Appointment.builder()
                .referenceNo("APT-" + now.format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .customer(reservation.getCustomer())
                .consultant(consultant)
                .branch(reservation.getBranch())
                .service(reservation.getService())
                .reservation(reservation)
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(AppointmentStatus.CONFIRMED)
                .build();

        reservation.transitionTo(ReservationStatus.CONFIRMED, now);
        reservationRepository.flush();
        appointmentRepository.saveAndFlush(appointment);
        return new ConfirmationResult(mapper.toResponse(appointment), true);
    }

    @Override
    @Transactional
    public int expireStaleReservations() {
        return reservationRepository.expireAllStale(OffsetDateTime.now(clock));
    }

    private Reservation loadAuthorized(UUID reservationId, String token) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(RESERVATION, reservationId));
        if (!tokenService.matches(token, reservation.getTokenHash())) {
            throw new ResourceNotFoundException(RESERVATION, reservationId);
        }
        return reservation;
    }

    private void expireIfStale(Reservation reservation, OffsetDateTime now) {
        if (reservation.isStaleAt(now)) {
            reservation.transitionTo(ReservationStatus.EXPIRED, now);
        }
    }

    private void validateServiceEligibility(Branch branch, Service service, Customer customer) {
        if (!branchServiceLinkRepository.existsByBranchIdAndServiceId(branch.getId(), service.getId())) {
            log.error("Service {} is not offered at branch {}", service.getId(), branch.getId());
            throw new InvalidArgumentException("The service is not offered at the selected branch.");
        }

        boolean allowed = customer.getCustomerType() == CustomerType.CLIENT
                ? service.isAvailableToRegistered() : service.isAvailableToGuest();
        if (!allowed) {
            log.error("Service {} is not available to customer type {}", service.getId(), customer.getCustomerType());
            throw new InvalidArgumentException("The service is not available to this customer type.");
        }
    }

    private @NonNull Customer getCustomer(CreateReservationRequest request) {
        return customerRepository.findById(request.customerId())
                .orElseThrow(() -> {
                    log.error("Customer not found: id={}", request.customerId());
                    return new ResourceNotFoundException("Customer", request.customerId());
                });
    }

    private @NonNull Service getService(CreateReservationRequest request) {
        return servicesRepository.findById(request.serviceId())
                .filter(Service::isActive)
                .orElseThrow(() -> {
                    log.error("Active service not found: id={}", request.serviceId());
                    return new ResourceNotFoundException("Service", request.serviceId());
                });
    }

    private @NonNull Branch getBranch(CreateReservationRequest request) {
        return branchRepository.findById(request.branchId())
                .orElseThrow(() -> {
                    log.error("Branch not found: id={}", request.branchId());
                    return new ResourceNotFoundException("Branch", request.branchId());
                });
    }
}
