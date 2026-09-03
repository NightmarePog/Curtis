package com.sosehl.curtis.feature.quizzes.core;

import java.util.UUID;

public record OptionSnapshot(
    UUID optionId,
    int position,
    String text,
    boolean correct
) {}
