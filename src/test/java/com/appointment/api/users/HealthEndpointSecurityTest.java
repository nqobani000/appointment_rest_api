package com.appointment.api.users;

import com.appointment.api.users.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real security filter chain; no handler exists for these paths, so "permitted" shows as 404 and "denied" as 401. */
@SpringJUnitWebConfig(classes = {SecurityConfig.class, HealthEndpointSecurityTest.WebConfig.class})
@TestPropertySource(properties = "app.security.jwt.secret=0123456789abcdef0123456789abcdef")
class HealthEndpointSecurityTest {

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class WebConfig {
    }

    @Autowired WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void healthProbesArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isNotFound());
    }

    @Test
    void otherActuatorPathsStayDenied() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyGetIsPermittedOnHealth() throws Exception {
        mockMvc.perform(post("/actuator/health")).andExpect(status().isUnauthorized());
    }
}
