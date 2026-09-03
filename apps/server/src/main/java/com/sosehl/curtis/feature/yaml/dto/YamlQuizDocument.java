package com.sosehl.curtis.feature.yaml.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
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

@JsonInclude(JsonInclude.Include.NON_NULL)
public record YamlQuizDocument(
    @JsonPropertyDescription("Curtis YAML schema version. Currently 1.")
    @Min(1) @Max(1) Integer schemaVersion,
    @JsonPropertyDescription("Teacher username used only by watched package imports.")
    @Size(max = 320) String ownerUsername,
    @JsonPropertyDescription("Existing quiz identifier; omit when creating a quiz.")
    UUID quizId,
    @JsonPropertyDescription("Optimistic-lock version; required when replacing a quiz.")
    @Min(0) Long version,
    @NotBlank @Size(max = 100) String title,
    @Size(max = 4000) String description,
    @NotNull UUID subjectId,
    @Size(max = 100) String chapter,
    QuizStatus status,
    @JsonPropertyDescription("Maximum number of questions selected for a session.")
    @NotNull @Min(1) @Max(100) Integer maxQuestionsPerSession,
    Boolean shuffle,
    Instant validFrom,
    Instant validTo,
    @NotNull @Size(min = 1, max = 100)
    List<@NotNull @Valid YamlQuestionDocument> questions
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YamlQuestionDocument(
        UUID id,
        QuestionType type,
        @JsonAlias("question") @NotBlank @Size(max = 2000) String prompt,
        @Min(1) @Max(100) Integer points,
        @Size(max = 20000) String codeSnippet,
        @JsonAlias({ "imageRef", "media" })
        @JsonPropertyDescription(
            "Uploaded media UUID for web requests, or asset filename for watched packages."
        )
        @Size(max = 255) String image,
        @JsonAlias("timeInSeconds")
        @JsonPropertyDescription("Answer time limit in seconds.")
        @Min(1) @Max(3600) Integer timeSeconds,
        @JsonPropertyDescription("Answer options for a multiple-choice question.")
        @Size(max = 100) List<@NotNull @Valid YamlOptionDocument> options,
        @JsonPropertyDescription("Pairs for a matching question.")
        @Size(max = 100) List<@NotNull @Valid YamlPairDocument> pairs
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YamlOptionDocument(
        UUID id,
        @NotBlank @Size(max = 1000) String text,
        Boolean correct
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record YamlPairDocument(
        UUID id,
        @NotBlank @Size(max = 1000) String left,
        @NotBlank @Size(max = 1000) String right
    ) {}
}
