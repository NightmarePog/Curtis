package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.SessionExpiredException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private final UUID uuid;
    private final String adminId;
    private final QuizGetResponse quiz;
    private final LocalDateTime expiresAt;
    private final Map<String, StudentAttempt> attempts =
        new ConcurrentHashMap<>();

    public Session(
        String adminId,
        QuizGetResponse quiz,
        Integer expiresInMinutes
    ) {
        if (adminId == null) throw new IllegalArgumentException(
            "adminId is missing"
        );
        if (quiz == null) throw new IllegalArgumentException("quiz is missing");
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("Quiz nemá žádné otázky");
        }

        this.uuid = UUID.randomUUID();
        this.adminId = adminId;
        this.quiz = quiz;
        this.expiresAt = LocalDateTime.now().plusMinutes(
            expiresInMinutes != null ? expiresInMinutes : 30
        );
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getAdminId() {
        return adminId;
    }

    public void join(String studentId) {
        if (isExpired()) throw new SessionExpiredException("Session vypršela");
        if (attempts.containsKey(studentId)) {
            throw new IllegalStateException("Student již session hrál");
        }

        List<QuestionResponse> questions = new ArrayList<>(quiz.getQuestions());
        if (Boolean.TRUE.equals(quiz.getShuffle())) Collections.shuffle(
            questions
        );

        Integer max = quiz.getMaxQuestionsPerSession();
        if (max != null && max < questions.size()) {
            questions = questions.subList(0, max);
        }

        attempts.put(studentId, new StudentAttempt(studentId, questions));
    }

    public QuestionResponse nextQuestion(String studentId) {
        if (isExpired()) throw new SessionExpiredException("Session vypršela");
        return getAttempt(studentId).nextQuestion();
    }

    public void submitAnswer(String studentId, List<Integer> answer) {
        getAttempt(studentId).addAnswer(answer);
    }

    public StudentAttempt finishAttempt(String studentId) {
        return getAttempt(studentId).finish();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    private StudentAttempt getAttempt(String studentId) {
        StudentAttempt attempt = attempts.get(studentId);
        if (attempt == null) throw new IllegalStateException(
            "Student není připojen k session"
        );
        return attempt;
    }
}
