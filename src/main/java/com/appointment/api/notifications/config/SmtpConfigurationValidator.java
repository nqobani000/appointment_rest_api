package com.appointment.api.notifications.config;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stops start-up when the SMTP sender is selected without credentials, so a half-configured container fails
 * loudly instead of quietly failing every email. A later delivery or connection problem stays non-fatal.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.notifications", name = "sender", havingValue = "smtp")
public class SmtpConfigurationValidator {

    private final JavaMailSender mailSender;

    public SmtpConfigurationValidator(JavaMailSender mailSender,
                                      @Value("${spring.mail.host:}") String host,
                                      @Value("${spring.mail.username:}") String username,
                                      @Value("${spring.mail.password:}") String password) {
        validate(host, username, password);
        this.mailSender = mailSender;
    }

    public static void validate(String host, String username, String password) {
        List<String> missing = new ArrayList<>();
        if (isBlank(host)) missing.add("MAIL_HOST");
        if (isBlank(username)) missing.add("MAIL_USERNAME");
        if (isBlank(password)) missing.add("MAIL_PASSWORD");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("NOTIFICATIONS_SENDER is 'smtp' but " + String.join(", ", missing)
                    + (missing.size() == 1 ? " is" : " are")
                    + " not set. Set them, or use NOTIFICATIONS_SENDER=logging for development.");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkConnection() {
        if (!(mailSender instanceof JavaMailSenderImpl impl)) return;
        try {
            impl.testConnection();
            log.info("SMTP connection to {} succeeded.", impl.getHost());
        } catch (MessagingException e) {
            log.warn("SMTP server {} is unreachable or rejected the credentials ({}). "
                    + "Emails stay queued and are retried by the outbox.", impl.getHost(), e.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
