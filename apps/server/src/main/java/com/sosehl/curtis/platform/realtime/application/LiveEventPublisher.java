package com.sosehl.curtis.platform.realtime.application;

import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LiveEventPublisher {
    private final ApplicationEventPublisher publisher;

    public LiveEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(LiveInvalidation event) { publisher.publishEvent(event); }
}
