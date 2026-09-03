package com.sosehl.curtis.feature.sessions.attempt.dto;

import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import java.time.Instant;
import java.util.UUID;

public record AttemptSummaryResponse(
    UUID id,
    UUID sessionId,
    String sessionTitle,
    UUID studentId,
    String studentName,
    int attemptNumber,
    AttemptStatus status,
    int score,
    int maxScore,
    int percentage,
    int pendingReviewCount,
    Instant startedAt,
    Instant submittedAt
) {}
