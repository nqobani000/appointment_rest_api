package com.appointment.api.users.dto;

import java.time.OffsetDateTime;

public record GuestCodeResponse(String message, OffsetDateTime expiresAt) {
}
