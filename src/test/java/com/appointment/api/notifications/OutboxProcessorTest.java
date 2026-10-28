package com.appointment.api.notifications;

import com.appointment.api.notifications.config.NotificationProperties;
import com.appointment.api.notifications.dao.EmailOutbox;
import com.appointment.api.notifications.dao.OutboxStatus;
import com.appointment.api.notifications.repository.EmailOutboxRepository;
import com.appointment.api.notifications.sender.EmailSender;
import com.appointment.api.notifications.service.NotificationServiceImpl;
import com.appointment.api.notifications.service.OutboxProcessor;
import com.appointment.api.notifications.template.EmailTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutboxProcessorTest {

    @Mock EmailOutboxRepository repo;
    @Mock EmailSender sender;

    final Instant now = Instant.parse("2030-01-01T10:00:00Z");
    final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    final NotificationProperties props = new NotificationProperties("logging", "from@x", Duration.ofSeconds(10), 20, 3, Duration.ofSeconds(30));
    OutboxProcessor sut;
    EmailOutbox row;

    @BeforeEach
    void setUp() {
        sut = new OutboxProcessor(repo, sender, props, clock);
        row = new EmailOutbox();
        row.setDedupeKey("k");
        row.setRecipient("a@b.com");
        row.setTemplate(EmailTemplate.VERIFICATION_CODE);
        row.setVariables(Map.of("code", "123456", "minutes", "10"));
        when(repo.lockDue(any(), anyInt())).thenReturn(List.of(row));
    }

    @Test
    void sentRowIsMarkedSentAndSecretsCleared() {
        sut.processDue();
        verify(sender).send(any());
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(row.getVariables()).isNull();
        assertThat(row.getSentAt()).isEqualTo(now);
    }

    @Test
    void failureSchedulesExponentialBackoff() {
        doThrow(new IllegalStateException("smtp down")).when(sender).send(any());
        sut.processDue();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getNextAttemptAt()).isEqualTo(now.plusSeconds(30));
        row.setAttempts(1);
        sut.processDue();
        assertThat(row.getNextAttemptAt()).isEqualTo(now.plusSeconds(60));
        assertThat(row.getLastError()).isEqualTo("IllegalStateException");
    }

    @Test
    void failsPermanentlyAfterMaxAttempts() {
        doThrow(new IllegalStateException()).when(sender).send(any());
        row.setAttempts(2);
        sut.processDue();
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(row.getVariables()).isNull();
    }

    @Test
    void enqueueIsIdempotentPerDedupeKey() {
        NotificationServiceImpl service = new NotificationServiceImpl(repo, clock);
        when(repo.existsByDedupeKey("k")).thenReturn(false, true);
        EmailMessage msg = new EmailMessage("k", "a@b.com", EmailTemplate.VERIFICATION_CODE, Map.of("code", "1"));
        assertThat(service.enqueue(msg)).isTrue();
        assertThat(service.enqueue(msg)).isFalse();
        verify(repo, times(1)).save(any());
    }
}
