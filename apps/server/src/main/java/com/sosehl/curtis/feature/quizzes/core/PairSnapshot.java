package com.sosehl.curtis.feature.quizzes.core;

import java.util.UUID;

public record PairSnapshot(
    UUID pairId,
    int position,
    String left,
    String right
) {}
