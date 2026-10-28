package com.appointment.api.users;

import com.appointment.api.users.security.CorsConfig;
import com.appointment.api.users.security.CorsProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    final CorsConfigurationSource source = new CorsConfig()
            .corsConfigurationSource(new CorsProperties(List.of("http://localhost:5173"), Duration.ofHours(1)));

    CorsConfiguration forPath(String path) {
        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        return source.getCorsConfiguration(request);
    }

    @Test
    void allowsConfiguredOriginHeadersAndMethodsOnApiPaths() {
        CorsConfiguration c = forPath("/api/reservations");
        assertThat(c.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(c.checkOrigin("http://evil.example")).isNull();
        assertThat(c.checkHeaders(List.of("Authorization", "X-Reservation-Token", "Content-Type"))).hasSize(3);
        assertThat(c.checkHttpMethod(org.springframework.http.HttpMethod.DELETE)).isNotEmpty();
        assertThat(c.getAllowCredentials()).isFalse();
    }
}
