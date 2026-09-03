package com.sosehl.curtis.feature.sessions.attempt.dto;

import java.util.UUID;

public record MatchingPairResultResponse(
    UUID leftId,
    String left,
    UUID rightId,
    String right
) {}
