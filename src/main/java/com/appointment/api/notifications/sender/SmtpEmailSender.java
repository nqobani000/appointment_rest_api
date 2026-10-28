package com.appointment.api.notifications.sender;

import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Never logs the message body, which can contain secrets such as verification codes. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "sender", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.from());
        mail.setTo(message.recipient());
        mail.setSubject(message.template().subject());
        mail.setText(message.template().renderBody(message.variables()));
        mailSender.send(mail);
    }
}
