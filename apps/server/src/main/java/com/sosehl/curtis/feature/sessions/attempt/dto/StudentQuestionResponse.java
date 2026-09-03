package com.sosehl.curtis.feature.sessions.attempt.dto;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentQuestionResponse(
    UUID id,
    int position,
    QuestionType type,
    String prompt,
    int points,
    String codeSnippet,
    UUID mediaId,
    Instant deadlineAt,
    List<QuestionOptionResponse> options,
    List<MatchingItemResponse> leftItems,
    List<MatchingItemResponse> rightItems
) {}
