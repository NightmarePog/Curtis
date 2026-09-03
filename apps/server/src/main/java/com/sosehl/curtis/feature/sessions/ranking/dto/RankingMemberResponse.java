package com.sosehl.curtis.feature.sessions.ranking.dto;

public record RankingMemberResponse(
    String displayName,
    long score,
    long maxScore,
    int percentage,
    long attemptCount,
    int rank,
    boolean currentStudent
) {}
