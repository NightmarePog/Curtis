package com.sosehl.curtis.feature.sessions.dto;

import com.sosehl.curtis.feature.sessions.CreateSessionCommand;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SessionCreateRequest(
    @NotNull UUID quizId,
    @Size(max = 100) Set<@NotNull UUID> classIds,
    @Size(max = 100) Set<@NotNull UUID> groupIds,
    @NotNull @Future Instant closesAt,
    @Min(1) @Max(10) Integer maxAttempts,
    ScorePolicy scorePolicy
) {
    public Set<UUID> safeClassIds() { return classIds == null ? Set.of() : Set.copyOf(classIds); }
    public Set<UUID> safeGroupIds() { return groupIds == null ? Set.of() : Set.copyOf(groupIds); }
    public int effectiveMaxAttempts() { return maxAttempts == null ? 1 : maxAttempts; }
    public ScorePolicy effectiveScorePolicy() { return scorePolicy == null ? ScorePolicy.BEST : scorePolicy; }

    public CreateSessionCommand toCommand() {
        return new CreateSessionCommand(
            quizId,
            safeClassIds(),
            safeGroupIds(),
            closesAt,
            effectiveMaxAttempts(),
            effectiveScorePolicy()
        );
    }
}
