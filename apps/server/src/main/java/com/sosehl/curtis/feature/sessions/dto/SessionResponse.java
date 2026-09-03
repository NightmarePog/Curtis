package com.sosehl.curtis.feature.sessions.dto;

import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.core.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
    UUID id,
    UUID quizId,
    String title,
    String description,
    String subject,
    String chapter,
    String teacherName,
    SessionStatus status,
    Instant startedAt,
    Instant closesAt,
    int questionCount,
    int maxAttempts,
    ScorePolicy scorePolicy,
    List<TargetResponse> targets
) {}
