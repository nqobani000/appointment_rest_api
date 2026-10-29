package com.appointment.api.notifications.sender;

import com.appointment.api.notifications.EmailMessage;
import com.appointment.api.notifications.config.NotificationProperties;
import com.appointment.api.notifications.template.EmailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Never logs the message body, which can contain secrets such as verification codes. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.notifications", name = "sender", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Override
    public void send(EmailMessage message) {
        EmailTemplate template = message.template();
        Map<String, String> variables = message.variables();

        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(message.recipient());
            helper.setSubject(template.renderSubject(variables));
            helper.setText(template.renderText(variables), template.renderHtml(variables));
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build the email message.", e);
        }
        mailSender.send(mime);
    }
}
