package com.appointment.api.users.service;

import com.appointment.api.exceptions.AuthenticationFailedException;
import com.appointment.api.exceptions.ConflictException;
import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.NotificationService;
import com.appointment.api.notifications.template.EmailTemplate;
import com.appointment.api.users.CustomerType;
import com.appointment.api.users.config.GuestVerificationProperties;
import com.appointment.api.users.dao.Customer;
import com.appointment.api.users.dao.GuestVerification;
import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.GuestCodeRequest;
import com.appointment.api.users.dto.GuestCodeResponse;
import com.appointment.api.users.dto.GuestVerifyRequest;
import com.appointment.api.users.repository.CustomerRepository;
import com.appointment.api.users.repository.GuestVerificationRepository;
import com.appointment.api.users.repository.UserRepository;
import com.appointment.api.users.security.JwtProperties;
import com.appointment.api.users.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestVerificationServiceImpl implements GuestVerificationService {

    private static final String SENT_MESSAGE = "If the details are valid, a verification code has been sent to the email address.";
    private static final String INVALID_CODE = "The verification code is invalid or has expired.";

    private final GuestVerificationRepository verifications;
    private final CustomerRepository customers;
    private final UserRepository users;
    private final NotificationService notifications;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final GuestVerificationProperties properties;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public GuestCodeResponse requestCode(GuestCodeRequest request) {
        String email = normalize(request.email());
        rejectIfClientEmail(email);
        Instant now = clock.instant();

        GuestVerification latest = verifications.findFirstByEmailAndConsumedAtIsNullOrderByCreatedAtDesc(email).orElse(null);
        if (latest != null && latest.getExpiresAt().isAfter(now)
                && latest.getCreatedAt().plus(properties.resendCooldown()).isAfter(now)) {
            return new GuestCodeResponse(SENT_MESSAGE, latest.getExpiresAt().atOffset(ZoneOffset.UTC));
        }

        verifications.findActive(email).forEach(v -> v.setConsumedAt(now));

        String code = String.format("%06d", random.nextInt(1_000_000));

        GuestVerification verification = GuestVerification.builder()
                .email(email)
                .name(request.name().trim())
                .surname(request.surname().trim())
                .phone(request.phone().trim())
                .codeHash(hash(email, code))
                .createdAt(now)
                .expiresAt(now.plus(properties.codeTtl()))
                .build();

        verifications.saveAndFlush(verification);

        EmailMessage message = constructEmail(verification, code, email);
        notifications.enqueue(message);

        return new GuestCodeResponse(SENT_MESSAGE, verification.getExpiresAt().atOffset(ZoneOffset.UTC));
    }

    // Failed attempts must persist, so the attempt counter is not rolled back with the 401.
    @Override
    @Transactional(noRollbackFor = {AuthenticationFailedException.class, ConflictException.class})
    public AuthResponse verify(GuestVerifyRequest request) {
        String email = normalize(request.email());
        Instant now = clock.instant();

        GuestVerification verification = verifications.lockLatestActive(email)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_CODE));

        if (!verification.getExpiresAt().isAfter(now) || verification.getAttempts() >= properties.maxAttempts()) {
            verification.setConsumedAt(now);
            throw new AuthenticationFailedException(INVALID_CODE);
        }

        verification.setAttempts(verification.getAttempts() + 1);
        boolean matches = MessageDigest.isEqual(
                hash(email, request.code()).getBytes(StandardCharsets.UTF_8),
                verification.getCodeHash().getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            if (verification.getAttempts() >= properties.maxAttempts()) {
                verification.setConsumedAt(now);
            }
            throw new AuthenticationFailedException(INVALID_CODE);
        }

        verification.setConsumedAt(now);
        rejectIfClientEmail(email);

        Customer customer = customers.findByEmailIgnoreCaseAndCustomerType(email, CustomerType.GUEST)
                .orElseGet(Customer::new);
        customer.setName(verification.getName());
        customer.setSurname(verification.getSurname());
        customer.setEmail(email);
        customer.setPhone(verification.getPhone());
        customer.setCustomerType(CustomerType.GUEST);

        try {
            customers.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Another verification for this email just completed. Please try again.");
        }
        return AuthResponse.of(jwtService.issue(customer), customer);
    }

    private void rejectIfClientEmail(String email) {
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("This email belongs to a client account. Please log in as a client.");
        }
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // Keyed with the app secret so a leaked table cannot be brute-forced offline from the 6-digit space alone.
    private String hash(String email, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private @NonNull EmailMessage constructEmail(GuestVerification verification, String code, String email) {
        String dedupeKey = "guest-verification:" + verification.getId();
        String ttl = String.valueOf(properties.codeTtl().toMinutes());
        Map<String, String> codeResponse = Map.of("code", code, "minutes", ttl);

        return new EmailMessage(
                dedupeKey, email,
                EmailTemplate.VERIFICATION_CODE,
                codeResponse);
    }
}