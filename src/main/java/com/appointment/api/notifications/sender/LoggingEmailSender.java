package com.appointment.api.notifications.sender;

import com.appointment.api.notifications.EmailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development sender: prints the rendered email instead of delivering it. Never use outside dev. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.notifications", name = "sender", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.info("[DEV EMAIL] to={} subject={} body={}", message.recipient(),
                message.template().renderSubject(message.variables()),
                message.template().renderText(message.variables()));
    }
}
