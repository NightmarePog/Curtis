package com.sosehl.curtis.platform.realtime.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis.platform.realtime.infrastructure.EventStreamProperties;
import com.sosehl.curtis.platform.realtime.domain.LiveEventType;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import com.sosehl.curtis.feature.users.core.UserAccessRevoked;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class LiveEventRegistryTest {

    @Test
    void targetedInvalidationOnlyReachesItsRecipients() {
        TestRegistry registry = new TestRegistry();
        UUID recipient = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        RecordingEmitter recipientEmitter = registry.subscribeRecording(recipient);
        RecordingEmitter unrelatedEmitter = registry.subscribeRecording(unrelated);
        recipientEmitter.clear();
        unrelatedEmitter.clear();

        registry.onInvalidation(
            LiveInvalidation.to(LiveEventType.RESULTS_CHANGED, Set.of(recipient))
        );

        assertThat(recipientEmitter.payloads())
            .singleElement()
            .satisfies(payload -> {
                assertThat(payload).contains("results-changed");
                assertThat(payload).contains("{}");
            });
        assertThat(unrelatedEmitter.payloads()).isEmpty();
    }

    @Test
    void revocationCompletesStreamsAndPreventsLaterDelivery() {
        TestRegistry registry = new TestRegistry();
        UUID userId = UUID.randomUUID();
        RecordingEmitter emitter = registry.subscribeRecording(userId);
        emitter.clear();

        registry.onUserAccessRevoked(new UserAccessRevoked(userId));
        registry.onInvalidation(
            LiveInvalidation.to(LiveEventType.RESULTS_CHANGED, Set.of(userId))
        );

        assertThat(emitter.completed()).isTrue();
        assertThat(emitter.payloads()).isEmpty();
    }

    @Test
    void disconnectingLoginIdentityCompletesItsAccountStreams() {
        TestRegistry registry = new TestRegistry();
        UUID userId = UUID.randomUUID();
        String identityKey = "issuer\nsubject";
        RecordingEmitter emitter = registry.subscribeRecording(
            userId,
            identityKey
        );

        registry.disconnectIdentity(identityKey);

        assertThat(emitter.completed()).isTrue();
    }

    private static final class TestRegistry extends LiveEventRegistry {
        private final ArrayDeque<RecordingEmitter> prepared = new ArrayDeque<>();

        private TestRegistry() {
            super(new EventStreamProperties(0, 25_000));
        }

        private RecordingEmitter subscribeRecording(UUID userId) {
            return subscribeRecording(userId, "identity:" + userId);
        }

        private RecordingEmitter subscribeRecording(
            UUID userId,
            String identityKey
        ) {
            RecordingEmitter emitter = new RecordingEmitter();
            prepared.add(emitter);
            assertThat(subscribe(userId, identityKey)).isSameAs(emitter);
            return emitter;
        }

        @Override
        SseEmitter createEmitter() {
            return prepared.remove();
        }
    }

    private static final class RecordingEmitter extends SseEmitter {
        private final List<String> payloads = new ArrayList<>();
        private boolean completed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            StringBuilder payload = new StringBuilder();
            builder.build().forEach(part -> payload.append(part.getData()));
            payloads.add(payload.toString());
        }

        @Override
        public void complete() {
            completed = true;
        }

        private List<String> payloads() {
            return List.copyOf(payloads);
        }

        private void clear() {
            payloads.clear();
        }

        private boolean completed() {
            return completed;
        }
    }
}
