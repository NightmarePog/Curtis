package com.sosehl.curtis.feature.sessions.attempt.dto;

import java.util.List;

public record AttemptDetailResponse(
    AttemptSummaryResponse attempt,
    List<QuestionResultResponse> questions
) {}
