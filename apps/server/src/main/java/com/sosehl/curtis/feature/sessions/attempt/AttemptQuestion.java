package com.sosehl.curtis.feature.sessions.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.sosehl.curtis.feature.sessions.core.AttemptQuestionState;
import com.sosehl.curtis.feature.sessions.core.GradingState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attempt_questions")
public class AttemptQuestion {
    @Id
    private UUID id;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(nullable = false)
    private int position;

    @Column(name = "source_question_id", nullable = false)
    private UUID sourceQuestionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode questionSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode response;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptQuestionState state;

    @Column(name = "served_at")
    private Instant servedAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    @Column(name = "awarded_points")
    private Integer awardedPoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_state", nullable = false, length = 24)
    private GradingState gradingState;

    @Column(name = "graded_by")
    private UUID gradedBy;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Version
    private long version;

    protected AttemptQuestion() {}

    public AttemptQuestion(UUID id, UUID attemptId, int position,
                           UUID sourceQuestionId, JsonNode snapshot, int maxPoints) {
        this.id = id;
        this.attemptId = attemptId;
        this.position = position;
        this.sourceQuestionId = sourceQuestionId;
        this.questionSnapshot = snapshot;
        this.maxPoints = maxPoints;
        this.state = AttemptQuestionState.READY;
        this.gradingState = GradingState.AUTO_GRADED;
    }

    public UUID getId() { return id; }
    public UUID getAttemptId() { return attemptId; }
    public int getPosition() { return position; }
    public JsonNode getQuestionSnapshot() { return questionSnapshot; }
    public JsonNode getResponse() { return response; }
    public AttemptQuestionState getState() { return state; }
    public Instant getServedAt() { return servedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public Instant getAnsweredAt() { return answeredAt; }
    public int getMaxPoints() { return maxPoints; }
    public Integer getAwardedPoints() { return awardedPoints; }
    public GradingState getGradingState() { return gradingState; }

    public void serve(Instant now, Instant deadline) {
        if (state != AttemptQuestionState.READY) return;
        state = AttemptQuestionState.SERVED;
        servedAt = now;
        deadlineAt = deadline;
    }

    public void answer(JsonNode response, Integer awarded, GradingState grading, Instant now) {
        this.response = response;
        this.awardedPoints = awarded;
        this.gradingState = grading;
        this.state = AttemptQuestionState.ANSWERED;
        this.answeredAt = now;
    }

    public void timeOut(Instant now) {
        if (state != AttemptQuestionState.READY && state != AttemptQuestionState.SERVED) return;
        state = AttemptQuestionState.TIMED_OUT;
        awardedPoints = 0;
        gradingState = GradingState.AUTO_GRADED;
        answeredAt = now;
    }

    public void manuallyGrade(int points, UUID graderId, Instant now) {
        if (
            state != AttemptQuestionState.ANSWERED ||
            gradingState != GradingState.PENDING_REVIEW
        ) {
            throw new IllegalStateException(
                "Only an answered item pending review can be graded."
            );
        }
        awardedPoints = points;
        gradingState = GradingState.MANUALLY_GRADED;
        gradedBy = graderId;
        gradedAt = now;
    }
}
