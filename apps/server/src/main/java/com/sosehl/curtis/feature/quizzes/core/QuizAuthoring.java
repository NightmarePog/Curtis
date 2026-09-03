package com.sosehl.curtis.feature.quizzes.core;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Framework-free boundary for authoring quizzes from another feature.
 *
 * <p>The HTTP DTOs and persistence model stay private to the quizzes feature.
 * Importers can use this contract without depending on those implementation
 * details.</p>
 */
public interface QuizAuthoring {

    Quiz create(UUID teacherId, Draft draft);

    Quiz replace(UUID teacherId, UUID quizId, Draft draft);

    Quiz get(UUID teacherId, UUID quizId);

    record Draft(
        String title,
        String description,
        UUID subjectId,
        String chapter,
        QuizStatus status,
        int maxQuestionsPerSession,
        boolean shuffle,
        Instant validFrom,
        Instant validTo,
        Long expectedVersion,
        List<QuestionDraft> questions
    ) {}

    record QuestionDraft(
        UUID id,
        QuestionType type,
        String prompt,
        int points,
        String codeSnippet,
        UUID mediaId,
        int timeSeconds,
        List<OptionDraft> options,
        List<PairDraft> pairs
    ) {}

    record OptionDraft(UUID id, String text, boolean correct) {}

    record PairDraft(UUID id, String left, String right) {}

    record Quiz(
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
        List<Question> questions
    ) {}

    record Question(
        UUID id,
        int position,
        QuestionType type,
        String prompt,
        int points,
        String codeSnippet,
        UUID mediaId,
        int timeSeconds,
        List<Option> options,
        List<Pair> pairs
    ) {}

    record Option(UUID id, int position, String text, boolean correct) {}

    record Pair(UUID id, int position, String left, String right) {}
}
