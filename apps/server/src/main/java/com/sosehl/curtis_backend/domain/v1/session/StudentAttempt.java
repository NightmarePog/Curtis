package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.NoMoreQuestionsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StudentAttempt {

    private static final int GRACE_PERIOD_SECONDS = 2;

    private final String studentId;
    private final List<QuestionResponse> questions;
    private final List<List<Integer>> answers = new ArrayList<>();
    private final Clock clock;
    private int questionIndex = 0;
    private SessionStatus status = SessionStatus.RUNNING;
    private Instant servedAt;

    public StudentAttempt(String studentId, List<QuestionResponse> questions) {
        this(studentId, questions, Clock.systemDefaultZone());
    }

    public StudentAttempt(
        String studentId,
        List<QuestionResponse> questions,
        Clock clock
    ) {
        this.studentId = studentId;
        this.questions = questions;
        this.clock = clock;
    }

    public String getStudentId() {
        return studentId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public List<List<Integer>> getAnswers() {
        return answers;
    }

    public QuestionResponse nextQuestion() {
        if (status != SessionStatus.RUNNING) {
            throw new IllegalStateException("Pokus již byl ukončen");
        }
        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
            throw new NoMoreQuestionsException("Žádné další otázky");
        }

        QuestionResponse question = questions.get(questionIndex);
        questionIndex++;
        servedAt = clock.instant();

        if (questionIndex >= questions.size()) {
            this.status = SessionStatus.ARCHIVED;
        }

        return question;
    }

    public void addAnswer(List<Integer> answer) {
        if (isAnswerLate()) {
            answers.add(new ArrayList<>());
            return;
        }
        answers.add(answer);
    }

    private boolean isAnswerLate() {
        if (servedAt == null || questionIndex == 0) {
            return false;
        }

        Integer timeInSeconds = questions
            .get(questionIndex - 1)
            .getTimeInSeconds();
        if (timeInSeconds == null) {
            return false;
        }

        Duration allowed = Duration.ofSeconds(
            timeInSeconds + GRACE_PERIOD_SECONDS
        );
        Duration elapsed = Duration.between(servedAt, clock.instant());
        return elapsed.compareTo(allowed) > 0;
    }

    public StudentAttempt finish() {
        this.status = SessionStatus.ARCHIVED;
        return this;
    }

    public int calculateScore() {
        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            List<QuestionAnswer> answers = questions.get(i).getAnswers();
            List<Integer> correct = new ArrayList<>();
            for (int j = 0; j < answers.size(); j++) {
                if (
                    Boolean.TRUE.equals(answers.get(j).getIsCorrect())
                ) correct.add(j);
            }
            List<Integer> userAnswer =
                this.answers.size() > i
                    ? this.answers.get(i)
                    : new ArrayList<>();
            if (
                new HashSet<>(correct).equals(new HashSet<>(userAnswer))
            ) score++;
        }
        return score;
    }
}
