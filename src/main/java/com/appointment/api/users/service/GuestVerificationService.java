package com.appointment.api.users.service;

import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.GuestCodeRequest;
import com.appointment.api.users.dto.GuestCodeResponse;
import com.appointment.api.users.dto.GuestVerifyRequest;

public interface GuestVerificationService {

    GuestCodeResponse requestCode(GuestCodeRequest request);

    AuthResponse verify(GuestVerifyRequest request);
}
