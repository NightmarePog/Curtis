package com.sosehl.curtis.feature.sessions.attempt;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttemptScoreCalculatorTest {

    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID BEST_SESSION_ID = UUID.randomUUID();
    private static final UUID LATEST_SESSION_ID = UUID.randomUUID();
    private static final UUID ALL_SESSION_ID = UUID.randomUUID();
    private static final Instant STARTED_AT = Instant.parse(
        "2026-09-03T08:00:00Z"
    );

    @Test
    void appliesEachSessionsPolicyBeforeCombiningScores() {
        List<Attempt> attempts = List.of(
            graded(BEST_SESSION_ID, 1, 8, 10, 1),
            graded(BEST_SESSION_ID, 2, 9, 20, 2),
            graded(LATEST_SESSION_ID, 1, 10, 10, 3),
            graded(LATEST_SESSION_ID, 2, 2, 10, 4),
            graded(ALL_SESSION_ID, 1, 3, 5, 5),
            graded(ALL_SESSION_ID, 2, 4, 5, 6)
        );

        var totals = AttemptScoreCalculator.byStudent(
            attempts,
            Map.of(
                BEST_SESSION_ID,
                ScorePolicy.BEST,
                LATEST_SESSION_ID,
                ScorePolicy.LATEST,
                ALL_SESSION_ID,
                ScorePolicy.ALL
            )
        );

        assertThat(totals.get(STUDENT_ID))
            .satisfies(total -> {
                assertThat(total.attempts()).isEqualTo(4);
                assertThat(total.score()).isEqualTo(17);
                assertThat(total.maxScore()).isEqualTo(30);
                assertThat(total.percentage()).isEqualTo(57);
                assertThat(total.lastActivity())
                    .isEqualTo(STARTED_AT.plusSeconds(6));
            });
    }

    @Test
    void treatsAnAttemptWithoutPossiblePointsAsWorseThanAScoredAttempt() {
        Attempt zeroPoint = graded(BEST_SESSION_ID, 2, 0, 0, 2);
        Attempt scored = graded(BEST_SESSION_ID, 1, 1, 2, 1);

        var total = AttemptScoreCalculator
            .byStudent(
                List.of(zeroPoint, scored),
                Map.of(BEST_SESSION_ID, ScorePolicy.BEST)
            )
            .get(STUDENT_ID);

        assertThat(total.score()).isEqualTo(1);
        assertThat(total.maxScore()).isEqualTo(2);
    }

    private Attempt graded(
        UUID sessionId,
        int attemptNumber,
        int score,
        int maxScore,
        long submittedAfterSeconds
    ) {
        Attempt attempt = new Attempt(
            UUID.randomUUID(),
            sessionId,
            STUDENT_ID,
            attemptNumber,
            STARTED_AT,
            maxScore
        );
        attempt.finalizeAttempt(
            AttemptStatus.GRADED,
            score,
            0,
            STARTED_AT.plusSeconds(submittedAfterSeconds)
        );
        return attempt;
    }
}
