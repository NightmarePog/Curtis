package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.shared.errors.ProblemException;

public final class QuizErrors {

    private QuizErrors() {}

    static ProblemException notFound() {
        return ProblemException.notFound("quiz_not_found", "Quiz not found.");
    }

    public static ProblemException invalid(String message) {
        return ProblemException.badRequest("quiz_invalid", message);
    }

    static ProblemException conflict(String message) {
        return ProblemException.conflict("quiz_conflict", message);
    }
}
