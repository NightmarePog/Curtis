package com.sosehl.curtis.feature.sessions.attempt.dto;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.attempt.AnswerCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record AnswerRequest(
    @NotNull QuestionType type,
    @Size(max = 100) List<@NotNull UUID> optionIds,
    @Size(max = 100) List<@NotNull @Valid MatchAnswer> pairs,
    @Size(max = 10000) String text
) {
    public List<UUID> safeOptionIds() { return optionIds == null ? List.of() : List.copyOf(optionIds); }
    public List<MatchAnswer> safePairs() { return pairs == null ? List.of() : List.copyOf(pairs); }

    public AnswerCommand toCommand() {
        return new AnswerCommand(
            type,
            safeOptionIds(),
            safePairs().stream()
                .map(pair -> new AnswerCommand.Match(pair.leftId(), pair.rightId()))
                .toList(),
            text
        );
    }
}
