package com.appointment.api.reservations.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReservationProperties.class)
public class ReservationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
