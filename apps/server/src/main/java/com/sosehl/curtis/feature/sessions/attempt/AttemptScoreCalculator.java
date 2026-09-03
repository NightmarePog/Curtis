package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Applies each session's attempt policy before scores are combined. */
public final class AttemptScoreCalculator {

    private AttemptScoreCalculator() {}

    public static Map<UUID, ScoreTotal> byStudent(
        Collection<Attempt> attempts,
        Map<UUID, ScorePolicy> policies
    ) {
        Map<StudentSession, List<Attempt>> grouped = new HashMap<>();
        attempts.forEach(attempt ->
            grouped
                .computeIfAbsent(
                    new StudentSession(
                        attempt.getStudentId(),
                        attempt.getSessionId()
                    ),
                    ignored -> new ArrayList<>()
                )
                .add(attempt)
        );

        Map<UUID, MutableScoreTotal> totals = new HashMap<>();
        grouped.forEach((key, values) -> {
            ScorePolicy policy = policies.get(key.sessionId());
            if (policy == null) return;
            selected(values, policy).forEach(attempt ->
                totals
                    .computeIfAbsent(
                        key.studentId(),
                        ignored -> new MutableScoreTotal()
                    )
                    .add(attempt)
            );
        });

        Map<UUID, ScoreTotal> result = new HashMap<>();
        totals.forEach((studentId, total) ->
            result.put(studentId, total.snapshot())
        );
        return Map.copyOf(result);
    }

    private static List<Attempt> selected(
        List<Attempt> values,
        ScorePolicy policy
    ) {
        if (policy == ScorePolicy.ALL) return List.copyOf(values);

        Comparator<Attempt> comparator = policy == ScorePolicy.LATEST
            ? Comparator.comparingInt(Attempt::getAttemptNumber)
            : Comparator
                .comparingDouble(AttemptScoreCalculator::scoreRatio)
                .thenComparingInt(Attempt::getScore)
                .thenComparingInt(Attempt::getAttemptNumber);
        return values.stream().max(comparator).stream().toList();
    }

    private static double scoreRatio(Attempt attempt) {
        return attempt.getMaxScore() == 0
            ? -1.0
            : (double) attempt.getScore() / attempt.getMaxScore();
    }

    public record ScoreTotal(
        long attempts,
        long score,
        long maxScore,
        Instant lastActivity
    ) {
        public static final ScoreTotal EMPTY = new ScoreTotal(0, 0, 0, null);

        public int percentage() {
            return maxScore == 0
                ? 0
                : (int) Math.round(score * 100.0 / maxScore);
        }
    }

    private record StudentSession(UUID studentId, UUID sessionId) {}

    private static final class MutableScoreTotal {
        private long attempts;
        private long score;
        private long maxScore;
        private Instant lastActivity;

        private void add(Attempt attempt) {
            attempts++;
            score += attempt.getScore();
            maxScore += attempt.getMaxScore();
            Instant submittedAt = attempt.getSubmittedAt();
            if (
                submittedAt != null &&
                (lastActivity == null || submittedAt.isAfter(lastActivity))
            ) {
                lastActivity = submittedAt;
            }
        }

        private ScoreTotal snapshot() {
            return new ScoreTotal(attempts, score, maxScore, lastActivity);
        }
    }
}
