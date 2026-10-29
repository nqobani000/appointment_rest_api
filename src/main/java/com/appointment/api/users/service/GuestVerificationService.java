package com.appointment.api.users.service;

import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.GuestCodeRequest;
import com.appointment.api.users.dto.GuestCodeResponse;
import com.appointment.api.users.dto.GuestVerifyRequest;

public interface GuestVerificationService {

    /**
     * Request a guest verification code to be sent to the provided email address.
     * @param request The request containing the email address for which to send the verification code.
     * @return A response containing the status of the request and any relevant information.
     */
    GuestCodeResponse requestCode(GuestCodeRequest request);

    /**
     * Verify the provided guest verification code and authenticate the guest user.
     * @param request The request containing the email address and verification code to be verified.
     * @return An authentication response containing the JWT token and user information if verification is successful.
     */
    AuthResponse verify(GuestVerifyRequest request);
}
