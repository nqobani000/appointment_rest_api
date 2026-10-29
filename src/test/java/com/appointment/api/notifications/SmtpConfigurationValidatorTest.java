package com.appointment.api.notifications;

import com.appointment.api.notifications.config.SmtpConfigurationValidator;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SmtpConfigurationValidatorTest {

    @Test
    void acceptsCompleteConfiguration() {
        assertThatCode(() -> SmtpConfigurationValidator.validate("smtp.example.com", "user", "secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void namesEveryMissingSetting() {
        assertThatThrownBy(() -> SmtpConfigurationValidator.validate("", null, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_HOST")
                .hasMessageContaining("MAIL_USERNAME")
                .hasMessageContaining("MAIL_PASSWORD");
    }

    @Test
    void namesOnlyTheMissingSetting() {
        assertThatThrownBy(() -> SmtpConfigurationValidator.validate("smtp.example.com", "user", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAIL_PASSWORD is not set")
                .hasMessageNotContaining("MAIL_HOST")
                .hasMessageNotContaining("MAIL_USERNAME");
    }

    @Test
    void constructorRejectsMissingCredentials() {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        assertThatThrownBy(() -> new SmtpConfigurationValidator(sender, "smtp.example.com", "", ""))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void connectionFailureAtStartupIsNotFatal() throws Exception {
        JavaMailSenderImpl sender = mock(JavaMailSenderImpl.class);
        doThrow(new MessagingException("535 bad credentials")).when(sender).testConnection();
        SmtpConfigurationValidator sut = new SmtpConfigurationValidator(sender, "smtp.example.com", "user", "secret");

        assertThatCode(sut::checkConnection).doesNotThrowAnyException();
        verify(sender).testConnection();
        assertThat(sut).isNotNull();
    }
}
