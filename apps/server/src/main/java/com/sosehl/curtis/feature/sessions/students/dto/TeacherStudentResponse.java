package com.sosehl.curtis.feature.sessions.students.dto;

import com.sosehl.curtis.feature.sessions.dto.TargetResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TeacherStudentResponse(
    UUID id,
    String displayName,
    List<TargetResponse> groups,
    long attemptCount,
    long score,
    long maxScore,
    int percentage,
    Instant lastActivity
) {}
