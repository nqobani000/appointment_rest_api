package com.appointment.api.users.controller;

import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.GuestCodeRequest;
import com.appointment.api.users.dto.GuestCodeResponse;
import com.appointment.api.users.dto.GuestVerifyRequest;
import com.appointment.api.users.service.GuestVerificationService;
import com.appointment.api.users.dto.LoginRequest;
import com.appointment.api.users.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GuestVerificationService guestVerificationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/guests/code")
    public ResponseEntity<GuestCodeResponse> requestGuestCode(@Valid @RequestBody GuestCodeRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(guestVerificationService.requestCode(request));
    }

    @PostMapping("/guests/verify")
    public ResponseEntity<AuthResponse> verifyGuest(@Valid @RequestBody GuestVerifyRequest request) {
        return ResponseEntity.ok(guestVerificationService.verify(request));
    }
}
