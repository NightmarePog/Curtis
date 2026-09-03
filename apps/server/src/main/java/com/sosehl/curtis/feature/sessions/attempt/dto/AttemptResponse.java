package com.sosehl.curtis.feature.sessions.attempt.dto;

import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import java.time.Instant;
import java.util.UUID;

public record AttemptResponse(
    UUID id,
    UUID sessionId,
    int attemptNumber,
    AttemptStatus status,
    int answeredQuestions,
    int totalQuestions,
    Integer score,
    int maxScore,
    int pendingReviewCount,
    Instant startedAt,
    Instant submittedAt,
    StudentQuestionResponse currentQuestion,
    boolean readyToSubmit
) {}
