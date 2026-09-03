package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateSessionCommand(
    UUID quizId,
    Set<UUID> classIds,
    Set<UUID> groupIds,
    Instant closesAt,
    int maxAttempts,
    ScorePolicy scorePolicy
) {
    public CreateSessionCommand {
        classIds = classIds == null ? Set.of() : Set.copyOf(classIds);
        groupIds = groupIds == null ? Set.of() : Set.copyOf(groupIds);
    }
}
