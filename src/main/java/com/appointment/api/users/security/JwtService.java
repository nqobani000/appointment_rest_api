package com.appointment.api.users.security;

import com.appointment.api.users.dao.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtService {

    public static final String TYPE_CLAIM = "type";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public IssuedToken issue(Customer customer) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.ttl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(customer.getId().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(TYPE_CLAIM, customer.getCustomerType().name())
                .build();
        JwtEncoderParameters parameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        return new IssuedToken(encoder.encode(parameters).getTokenValue(), expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
