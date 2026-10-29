package com.appointment.api.users.dto;

import com.appointment.api.users.CustomerType;
import com.appointment.api.users.dao.Customer;
import com.appointment.api.users.security.JwtService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        OffsetDateTime expiresAt,
        UUID customerId,
        CustomerType customerType) {

    public static AuthResponse of(JwtService.IssuedToken token, Customer customer) {
        return new AuthResponse(token.value(), "Bearer", token.expiresAt().atOffset(ZoneOffset.UTC),
                customer.getId(), customer.getCustomerType());
    }
}
