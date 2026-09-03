package com.sosehl.curtis.platform.realtime.application;

import static org.mockito.Mockito.verify;

import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class LiveEventPublisherTest {
    @Mock
    private ApplicationEventPublisher applicationEvents;

    @Test
    void delegatesInvalidationToSpringEvents() {
        LiveEventPublisher publisher = new LiveEventPublisher(applicationEvents);
        LiveInvalidation event = org.mockito.Mockito.mock(
            LiveInvalidation.class
        );

        publisher.publish(event);

        verify(applicationEvents).publishEvent(event);
    }
}
