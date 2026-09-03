package com.sosehl.curtis.feature.quizzes.dto;

import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.quizzes.core.QuizStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class QuizDtos {

    private QuizDtos() {}

    public record AdminQuizCreateRequest(
        @NotNull UUID creatorId,
        @NotNull @Valid QuizWriteRequest quiz
    ) {}

    public record QuizWriteRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 4000) String description,
        @NotNull UUID subjectId,
        @Size(max = 100) String chapter,
        @NotNull QuizStatus status,
        @Min(1) @Max(100) int maxQuestionsPerSession,
        boolean shuffle,
        Instant validFrom,
        Instant validTo,
        Long expectedVersion,
        @NotNull @Size(max = 100)
        List<@NotNull @Valid QuestionWriteRequest> questions
    ) {}

    public record QuestionWriteRequest(
        UUID id,
        @NotNull QuestionType type,
        @NotBlank @Size(max = 2000) String prompt,
        @Min(1) @Max(100) int points,
        @Size(max = 20000) String codeSnippet,
        UUID mediaId,
        @Min(1) @Max(3600) int timeSeconds,
        @NotNull @Size(max = 100)
        List<@NotNull @Valid OptionWriteRequest> options,
        @NotNull @Size(max = 100)
        List<@NotNull @Valid PairWriteRequest> pairs
    ) {}

    public record OptionWriteRequest(
        UUID id,
        @NotBlank @Size(max = 1000) String text,
        boolean correct
    ) {}

    public record PairWriteRequest(
        UUID id,
        @NotBlank @Size(max = 1000) String left,
        @NotBlank @Size(max = 1000) String right
    ) {}

    public record QuizResponse(
        UUID id,
        long version,
        UUID creatorId,
        UUID subjectId,
        String subjectName,
        String title,
        String description,
        String chapter,
        QuizStatus status,
        int maxQuestionsPerSession,
        boolean shuffle,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        List<QuestionResponse> questions
    ) {}

    public record QuestionResponse(
        UUID id,
        int position,
        QuestionType type,
        String prompt,
        int points,
        String codeSnippet,
        UUID mediaId,
        int timeSeconds,
        List<OptionResponse> options,
        List<PairResponse> pairs
    ) {}

    public record OptionResponse(UUID id, int position, String text, boolean correct) {}
    public record PairResponse(UUID id, int position, String left, String right) {}
    public record QuizIdResponse(UUID quizId, long version) {}
}
