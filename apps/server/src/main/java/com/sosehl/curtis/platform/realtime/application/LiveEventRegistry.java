package com.sosehl.curtis.platform.realtime.application;

import com.sosehl.curtis.platform.realtime.infrastructure.EventStreamProperties;
import com.sosehl.curtis.platform.realtime.domain.LiveInvalidation;
import com.sosehl.curtis.feature.users.core.UserAccessRevoked;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class LiveEventRegistry {
    private static final String EMPTY_DATA = "{}";
    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Map<String, UUID> identityUsers = new ConcurrentHashMap<>();
    private final long timeout;

    public LiveEventRegistry(EventStreamProperties properties) {
        this.timeout = properties.connectionTimeoutMs();
    }

    public synchronized SseEmitter subscribe(UUID userId, String identityKey) {
        identityUsers.put(identityKey, userId);
        Set<SseEmitter> userEmitters = emitters.computeIfAbsent(
            userId,
            ignored -> ConcurrentHashMap.newKeySet()
        );
        if (userEmitters.size() >= MAX_CONNECTIONS_PER_USER) {
            SseEmitter oldest = userEmitters.iterator().next();
            remove(userId, oldest);
            oldest.complete();
        }

        SseEmitter emitter = createEmitter();
        userEmitters.add(emitter);
        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        try {
            emitter.send(SseEmitter.event().name("connected").reconnectTime(3000).data(EMPTY_DATA));
        } catch (IOException | IllegalStateException exception) {
            cleanup.run();
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvalidation(LiveInvalidation event) {
        if (event.broadcast()) {
            Set.copyOf(emitters.keySet()).forEach(user -> send(user, event.type().wireName()));
        } else {
            event.recipients().forEach(user -> send(user, event.type().wireName()));
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserAccessRevoked(UserAccessRevoked event) {
        disconnectUser(event.userId());
    }

    /** Completes every open stream for an account and prevents later delivery to them. */
    public synchronized void disconnectUser(UUID userId) {
        Set<SseEmitter> values = emitters.remove(userId);
        identityUsers.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        if (values == null) return;
        Set.copyOf(values).forEach(SseEmitter::complete);
    }

    public synchronized void disconnectIdentity(String identityKey) {
        UUID userId = identityUsers.remove(identityKey);
        if (userId != null) disconnectUser(userId);
    }

    @Scheduled(fixedRateString = "${app.events.heartbeat-ms:25000}")
    public synchronized void heartbeat() {
        for (Map.Entry<UUID, Set<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : Set.copyOf(entry.getValue())) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                } catch (IOException | IllegalStateException exception) {
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }

    @PreDestroy
    synchronized void shutdown() {
        emitters.values().forEach(values -> values.forEach(SseEmitter::complete));
        emitters.clear();
        identityUsers.clear();
    }

    private synchronized void send(UUID userId, String event) {
        Set<SseEmitter> values = emitters.get(userId);
        if (values == null) return;
        for (SseEmitter emitter : Set.copyOf(values)) {
            try {
                emitter.send(SseEmitter.event().name(event).data(EMPTY_DATA));
            } catch (IOException | IllegalStateException exception) {
                remove(userId, emitter);
            }
        }
    }

    private synchronized void remove(UUID userId, SseEmitter emitter) {
        emitters.computeIfPresent(userId, (ignored, values) -> {
            values.remove(emitter);
            return values.isEmpty() ? null : values;
        });
        if (!emitters.containsKey(userId)) {
            identityUsers.entrySet().removeIf(entry -> entry.getValue().equals(userId));
        }
    }

    SseEmitter createEmitter() {
        return new SseEmitter(timeout);
    }
}
