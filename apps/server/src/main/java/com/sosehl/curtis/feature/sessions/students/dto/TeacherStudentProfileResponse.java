package com.sosehl.curtis.feature.sessions.students.dto;

import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.dto.TargetResponse;
import java.util.List;
import java.util.UUID;

public record TeacherStudentProfileResponse(
    UUID id,
    String displayName,
    UUID classId,
    String className,
    List<TargetResponse> groups,
    long attemptCount,
    long score,
    long maxScore,
    int percentage,
    List<AttemptSummaryResponse> attempts
) {}
