package com.sosehl.curtis.feature.sessions.ranking.dto;

import java.util.List;
import java.util.UUID;

public record RankingResponse(
    String type,
    UUID id,
    UUID classId,
    String name,
    List<RankingMemberResponse> members
) {}
