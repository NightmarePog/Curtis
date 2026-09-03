package com.sosehl.curtis.feature.sessions.attempt.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
    UUID questionResultId,
    UUID attemptId,
    UUID sessionId,
    String sessionTitle,
    UUID studentId,
    String studentName,
    String prompt,
    String answer,
    int maxPoints,
    Integer awardedPoints,
    Instant answeredAt
) {}
