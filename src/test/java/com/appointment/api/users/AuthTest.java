package com.appointment.api.users;

import com.appointment.api.exceptions.AuthenticationFailedException;
import com.appointment.api.users.dao.Customer;
import com.appointment.api.users.dao.User;
import com.appointment.api.users.dto.AuthResponse;
import com.appointment.api.users.dto.LoginRequest;
import com.appointment.api.users.repository.CustomerRepository;
import com.appointment.api.users.repository.UserRepository;
import com.appointment.api.users.security.JwtProperties;
import com.appointment.api.users.security.JwtService;
import com.appointment.api.users.security.SecurityConfig;
import com.appointment.api.users.service.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthTest {

    static final String PASSWORD = "Exi$tingcli3nt";

    @Mock UserRepository users;
    @Mock CustomerRepository customers;

    final SecurityConfig config = new SecurityConfig();
    final PasswordEncoder encoder = config.passwordEncoder();
    final JwtProperties props = new JwtProperties("0123456789abcdef0123456789abcdef", Duration.ofHours(1));
    final Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    JwtDecoder decoder;
    AuthServiceImpl sut;
    User user = new User();
    Customer client = new Customer();

    @BeforeEach
    void setUp() {
        decoder = config.jwtDecoder(props);
        sut = new AuthServiceImpl(users, customers, encoder, new JwtService(config.jwtEncoder(props), props, clock));
        sut.init();
        user.setId(UUID.randomUUID());
        user.setEmail("lerato@example.com");
        user.setPasswordHash(encoder.encode(PASSWORD));
        client.setId(UUID.randomUUID());
        client.setCustomerType(CustomerType.CLIENT);
        when(users.findByEmailIgnoreCase("lerato@example.com")).thenReturn(Optional.of(user));
        when(customers.findByUserId(user.getId())).thenReturn(Optional.of(client));
    }

    @Test
    void clientLoginIssuesJwtWithCustomerSubject() {
        AuthResponse res = sut.login(new LoginRequest("lerato@example.com", PASSWORD));
        Jwt jwt = decoder.decode(res.accessToken());
        assertThat(jwt.getSubject()).isEqualTo(client.getId().toString());
        assertThat(jwt.getClaimAsString(JwtService.TYPE_CLAIM)).isEqualTo("CLIENT");
        assertThat(res.customerType()).isEqualTo(CustomerType.CLIENT);
        assertThat(res.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void badPasswordAndUnknownEmailFailIdentically() {
        assertThatThrownBy(() -> sut.login(new LoginRequest("lerato@example.com", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password.");
        assertThatThrownBy(() -> sut.login(new LoginRequest("nobody@example.com", PASSWORD)))
                .isInstanceOf(AuthenticationFailedException.class).hasMessage("Invalid email or password.");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = sut.login(new LoginRequest("lerato@example.com", PASSWORD)).accessToken();
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(BadJwtException.class);
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
