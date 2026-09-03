package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import java.util.List;
import java.util.UUID;

public record AnswerCommand(
    QuestionType type,
    List<UUID> optionIds,
    List<Match> pairs,
    String text
) {
    public AnswerCommand {
        optionIds = optionIds == null ? List.of() : List.copyOf(optionIds);
        pairs = pairs == null ? List.of() : List.copyOf(pairs);
    }

    public record Match(UUID leftId, UUID rightId) {}
}
