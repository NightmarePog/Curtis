package com.sosehl.curtis.feature.sessions.attempt.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.core.AttemptQuestionState;
import com.sosehl.curtis.feature.sessions.core.GradingState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuestionResultResponse(
    UUID id,
    int position,
    String prompt,
    QuestionType type,
    int maxPoints,
    Integer awardedPoints,
    AttemptQuestionState state,
    GradingState gradingState,
    List<QuestionOptionResponse> options,
    List<MatchingPairResultResponse> matchingPairs,
    JsonNode response,
    JsonNode correctAnswer,
    Instant answeredAt
) {}
