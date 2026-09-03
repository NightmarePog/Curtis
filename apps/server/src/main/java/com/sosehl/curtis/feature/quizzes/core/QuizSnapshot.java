package com.sosehl.curtis.feature.quizzes.core;

import java.util.List;
import java.util.UUID;

public record QuizSnapshot(
    UUID quizId,
    long version,
    String title,
    String description,
    String subjectName,
    String chapter,
    int maxQuestionsPerSession,
    boolean shuffle,
    List<QuestionSnapshot> questions
) {
    public QuizSnapshot {
        questions = List.copyOf(questions);
    }
}
