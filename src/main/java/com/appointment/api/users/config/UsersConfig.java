package com.appointment.api.users.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GuestVerificationProperties.class)
public class UsersConfig {
}
