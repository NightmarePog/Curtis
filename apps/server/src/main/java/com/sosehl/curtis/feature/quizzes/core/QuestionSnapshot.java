package com.sosehl.curtis.feature.quizzes.core;

import java.util.List;
import java.util.UUID;

public record QuestionSnapshot(
    UUID questionId,
    int position,
    QuestionType type,
    String prompt,
    int points,
    String codeSnippet,
    UUID mediaId,
    int timeSeconds,
    List<OptionSnapshot> options,
    List<PairSnapshot> pairs
) {
    public QuestionSnapshot {
        options = List.copyOf(options);
        pairs = List.copyOf(pairs);
    }
}
