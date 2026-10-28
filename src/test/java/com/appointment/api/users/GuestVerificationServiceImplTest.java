package com.appointment.api.users;

import com.appointment.api.exceptions.AuthenticationFailedException;
import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.NotificationService;
import com.appointment.api.users.config.GuestVerificationProperties;
import com.appointment.api.users.dao.Customer;
import com.appointment.api.users.dao.GuestVerification;
import com.appointment.api.users.dao.User;
import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.GuestCodeRequest;
import com.appointment.api.users.dto.GuestVerifyRequest;
import com.appointment.api.users.repository.CustomerRepository;
import com.appointment.api.users.repository.GuestVerificationRepository;
import com.appointment.api.users.repository.UserRepository;
import com.appointment.api.users.security.JwtProperties;
import com.appointment.api.users.security.JwtService;
import com.appointment.api.users.security.SecurityConfig;
import com.appointment.api.users.service.GuestVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuestVerificationServiceImplTest {

    @Mock GuestVerificationRepository verifications;
    @Mock CustomerRepository customers;
    @Mock UserRepository users;
    @Mock NotificationService notifications;

    final SecurityConfig config = new SecurityConfig();
    final JwtProperties jwtProps = new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ofHours(1));
    final GuestVerificationProperties props = new GuestVerificationProperties(Duration.ofMinutes(10), 3, Duration.ofMinutes(1));
    final Instant start = Instant.parse("2030-01-01T10:00:00Z");
    final List<GuestVerification> store = new ArrayList<>();
    Instant now = start;
    GuestVerificationServiceImpl sut;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock() {
            public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return now; }
        };
        sut = new GuestVerificationServiceImpl(verifications, customers, users, notifications,
                new JwtService(config.jwtEncoder(jwtProps), jwtProps, clock), jwtProps, props, clock);
        when(verifications.saveAndFlush(any())).thenAnswer(i -> {
            GuestVerification v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            store.add(v);
            return v;
        });
        when(verifications.findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(any())).thenAnswer(i ->
                active().reduce((a, b) -> b));
        when(verifications.findActive(any())).thenAnswer(i -> active().toList());
        when(verifications.lockLatestActive(any())).thenAnswer(i -> active().reduce((a, b) -> b));
        when(customers.saveAndFlush(any())).thenAnswer(i -> {
            Customer c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
    }

    java.util.stream.Stream<GuestVerification> active() {
        return store.stream().filter(v -> v.getConsumedAt() == null);
    }

    GuestCodeRequest request(String email, String name) {
        return new GuestCodeRequest(name, "Smith", email, "0821234567");
    }

    String requestAndCaptureCode(String email, String name) {
        sut.requestCode(request(email, name));
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(notifications, atLeastOnce()).enqueue(captor.capture());
        return captor.getValue().variables().get("code");
    }

    @Test
    void requestCreatesPendingVerificationAndQueuesEmailWithoutCreatingCustomer() {
        String code = requestAndCaptureCode(" Zan@Example.com ", "Zan");
        assertThat(code).matches("\\d{6}");
        ArgumentCaptor<EmailMessage> queued = ArgumentCaptor.forClass(EmailMessage.class);
        verify(notifications).enqueue(queued.capture());
        assertThat(queued.getValue().variables()).containsEntry("expiresIn", "10 minutes");
        assertThat(store).hasSize(1);
        assertThat(store.get(0).getEmail()).isEqualTo("zan@example.com");
        assertThat(store.get(0).getCodeHash()).doesNotContain(code);
        verify(customers, never()).saveAndFlush(any());
    }

    @Test
    void requestForClientEmailIsRejected() {
        when(users.findByEmailIgnoreCase("lerato@example.com")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> sut.requestCode(request("lerato@example.com", "Zan")))
                .isInstanceOf(ConflictException.class);
        verify(notifications, never()).enqueue(any());
    }

    @Test
    void repeatWithinCooldownDoesNotSendAgain() {
        sut.requestCode(request("zan@example.com", "Zan"));
        now = now.plusSeconds(30);
        sut.requestCode(request("zan@example.com", "Zan"));
        verify(notifications, times(1)).enqueue(any());
        assertThat(store).hasSize(1);
    }

    @Test
    void resendAfterCooldownSupersedesEarlierCode() {
        String first = requestAndCaptureCode("zan@example.com", "Zan");
        now = now.plusSeconds(61);
        String second = requestAndCaptureCode("zan@example.com", "Zan");
        assertThat(store).hasSize(2);
        assertThat(store.get(0).getConsumedAt()).isNotNull();
        if (!first.equals(second)) {
            assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", first)))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
    }

    @Test
    void correctCodeCreatesGuestAndIssuesJwt() {
        String code = requestAndCaptureCode("zan@example.com", "Zan");
        AuthResponse res = sut.verify(new GuestVerifyRequest("Zan@example.com", code));
        assertThat(res.customerType()).isEqualTo(CustomerType.GUEST);
        assertThat(res.accessToken()).isNotBlank();
        verify(customers).saveAndFlush(argThat(c -> c.getEmail().equals("zan@example.com") && c.getUser() == null));
        assertThat(store.get(0).getConsumedAt()).isNotNull();
    }

    @Test
    void returningGuestKeepsCustomerIdAndOverwritesDetails() {
        Customer existing = new Customer();
        existing.setId(UUID.randomUUID());
        existing.setName("Old");
        existing.setSurname("Name");
        existing.setPhone("000");
        existing.setCustomerType(CustomerType.GUEST);
        when(customers.findByEmailIgnoreCaseAndCustomerType("zan@example.com", CustomerType.GUEST))
                .thenReturn(Optional.of(existing));

        String code = requestAndCaptureCode("zan@example.com", "Zanele");
        AuthResponse res = sut.verify(new GuestVerifyRequest("zan@example.com", code));

        assertThat(res.customerId()).isEqualTo(existing.getId());
        assertThat(existing.getName()).isEqualTo("Zanele");
        assertThat(existing.getPhone()).isEqualTo("0821234567");
    }

    @Test
    void wrongCodeFailsAndLocksOutAfterMaxAttempts() {
        String code = requestAndCaptureCode("zan@example.com", "Zan");
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", wrong)))
                    .isInstanceOf(AuthenticationFailedException.class);
        }
        assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", code)))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(customers, never()).saveAndFlush(any());
    }

    @Test
    void expiredCodeIsRejected() {
        String code = requestAndCaptureCode("zan@example.com", "Zan");
        now = now.plus(Duration.ofMinutes(10));
        assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", code)))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void codeCannotBeReused() {
        String code = requestAndCaptureCode("zan@example.com", "Zan");
        sut.verify(new GuestVerifyRequest("zan@example.com", code));
        assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", code)))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void verifyFailsIfEmailBecameAClientMeanwhile() {
        String code = requestAndCaptureCode("zan@example.com", "Zan");
        when(users.findByEmailIgnoreCase("zan@example.com")).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> sut.verify(new GuestVerifyRequest("zan@example.com", code)))
                .isInstanceOf(ConflictException.class);
        verify(customers, never()).saveAndFlush(any());
    }
}
