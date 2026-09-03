package com.sosehl.curtis.platform.realtime.domain;

import java.util.Set;
import java.util.UUID;

public record LiveInvalidation(LiveEventType type, Set<UUID> recipients, boolean broadcast) {
    public LiveInvalidation {
        recipients = recipients == null ? Set.of() : Set.copyOf(recipients);
    }

    public static LiveInvalidation to(LiveEventType type, Set<UUID> recipients) {
        return new LiveInvalidation(type, recipients, false);
    }

    public static LiveInvalidation broadcast(LiveEventType type) {
        return new LiveInvalidation(type, Set.of(), true);
    }
}
