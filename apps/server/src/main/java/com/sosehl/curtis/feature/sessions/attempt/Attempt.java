package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.QuizSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempts")
public class Attempt {
    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "session_id",
        insertable = false,
        updatable = false
    )
    private QuizSession session;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AttemptStatus status;

    @Column(name = "current_position", nullable = false)
    private int currentPosition;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(nullable = false)
    private int score;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(name = "pending_review_count", nullable = false)
    private int pendingReviewCount;

    @Version
    private long version;

    protected Attempt() {}

    public Attempt(UUID id, UUID sessionId, UUID studentId, int attemptNumber,
                   Instant startedAt, int maxScore) {
        this.id = id;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = startedAt;
        this.maxScore = maxScore;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }
    public int getAttemptNumber() { return attemptNumber; }
    public AttemptStatus getStatus() { return status; }
    public int getCurrentPosition() { return currentPosition; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public int getScore() { return score; }
    public int getMaxScore() { return maxScore; }
    public int getPendingReviewCount() { return pendingReviewCount; }
    public void advance() { currentPosition++; }

    public void finalizeAttempt(AttemptStatus terminal, int score,
                                int pendingReviewCount, Instant now) {
        if (status != AttemptStatus.IN_PROGRESS) return;
        this.status = terminal;
        this.score = score;
        this.pendingReviewCount = pendingReviewCount;
        this.submittedAt = now;
        if (terminal == AttemptStatus.GRADED) this.gradedAt = now;
    }

    public void applyGradeTotals(int score, int pending, Instant now) {
        if (status != AttemptStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                "Only a submitted attempt pending review can be graded."
            );
        }
        this.score = score;
        this.pendingReviewCount = pending;
        this.status = pending == 0 ? AttemptStatus.GRADED : AttemptStatus.PENDING_REVIEW;
        this.gradedAt = pending == 0 ? now : null;
    }
}
