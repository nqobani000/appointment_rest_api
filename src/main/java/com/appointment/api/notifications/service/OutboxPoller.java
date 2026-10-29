package com.appointment.api.notifications.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxProcessor processor;

    @Scheduled(fixedDelayString = "${app.notifications.poll-interval:PT10S}")
    public void poll() {
        processor.processDue();
    }
}
