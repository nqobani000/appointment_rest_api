package com.appointment.api.notifications;

import com.appointment.api.notifications.config.NotificationProperties;
import com.appointment.api.notifications.sender.SmtpEmailSender;
import com.appointment.api.notifications.template.EmailTemplate;
import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SmtpEmailSenderTest {

    final JavaMailSender mailSender = mock(JavaMailSender.class);
    final NotificationProperties props = new NotificationProperties("smtp", "no-reply@bank.test",
            Duration.ofSeconds(10), 20, 5, Duration.ofSeconds(30));
    final SmtpEmailSender sut = new SmtpEmailSender(mailSender, props);

    @Test
    void sendsMultipartWithHtmlAndTextAlternatives() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        sut.send(new EmailMessage("k", "jane@example.com", EmailTemplate.VERIFICATION_CODE,
                Map.of("code", "123456", "expiresIn", "10 minutes")));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        assertThat(sent.getFrom()).extracting(Address::toString).containsExactly("no-reply@bank.test");
        assertThat(sent.getAllRecipients()).extracting(Address::toString).containsExactly("jane@example.com");
        assertThat(sent.getSubject()).isEqualTo("Your access code");

        List<String> html = new ArrayList<>();
        List<String> text = new ArrayList<>();
        collect(sent, html, text);
        assertThat(html).containsExactly("Your access code is <b>123456</b> and it expires in <b>10 minutes</b>");
        assertThat(text).containsExactly("Your access code is 123456 and it expires in 10 minutes");
    }

    private static void collect(Part part, List<String> html, List<String> text) throws Exception {
        if (part.isMimeType("text/html")) {
            html.add((String) part.getContent());
        } else if (part.isMimeType("text/plain")) {
            text.add((String) part.getContent());
        } else if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                collect(multipart.getBodyPart(i), html, text);
            }
        }
    }
}
