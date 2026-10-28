package com.appointment.api.users.service;

import com.appointment.api.exceptions.AuthenticationFailedException;
import com.appointment.api.users.dao.Customer;
import com.appointment.api.users.dao.User;
import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.LoginRequest;
import com.appointment.api.users.repository.CustomerRepository;
import com.appointment.api.users.repository.UserRepository;
import com.appointment.api.users.security.JwtService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private String dummyHash;

    @PostConstruct
    public void init() {
        dummyHash = passwordEncoder.encode("unused-placeholder");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches) {
            throw new AuthenticationFailedException();
        }
        Customer customer = customerRepository.findByUserId(user.getId())
                .orElseThrow(AuthenticationFailedException::new);
        return toResponse(customer);
    }

        private AuthResponse toResponse(Customer customer) {
        return AuthResponse.of(jwtService.issue(customer), customer);
    }
}
