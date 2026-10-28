package com.appointment.api.users.service;

import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.LoginRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);
}
