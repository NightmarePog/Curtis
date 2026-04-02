package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.enums.SessionState;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class Session {

    private QuizGetResponse quiz;
    private String userID;
    private UUID uuid;
    private LocalDateTime expirationDate;
    private List<List<Integer>> userAnswers = new ArrayList<>();
    private Integer questionIndex = 0;

    public Session setQuiz(QuizGetResponse quiz) {
        this.quiz = quiz;
        return this;
    }

    public Session setUserId(String userId) {
        this.userID = userId;
        return this;
    }

    public void setSessionUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getSessionUuid(UUID uuid) {
        return this.uuid;
    }

    public String getUserID() {
        return userID;
    }

    public SessionState getState() {
        return this.state;
    }

    public Session build() {
        if (this.quiz == null) {
            throw new RuntimeException("quiz is missing");
        }
        if (this.userID == null) {
            throw new RuntimeException("userId is missing");
        }

        this.expirationDate = LocalDateTime.now().plusMinutes(
            this.quiz.getExpiresInMinutes() != null
                ? this.quiz.getExpiresInMinutes()
                : 30
        );

        this.state = SessionState.RUNNING;

        List<QuestionResponse> questions = new ArrayList<>(
            this.quiz.getQuestions()
        );
        Collections.shuffle(questions);
        this.quiz.setQuestions(questions);

        return this;
    }

    public void setUserAnswer(List<Integer> userAnswer) {
        this.userAnswers.add(userAnswer);
    }

    public QuestionResponse nextQuestion() {
        if (state != SessionState.RUNNING) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Kvíz momentálně není ve stavu 'LIVE'"
            );
        }

        if (
            expirationDate != null &&
            LocalDateTime.now().isAfter(expirationDate)
        ) {
            this.state = SessionState.ARCHIVED;
            throw new ResponseStatusException(HttpStatus.GONE, "Kvíz vypršel");
        }

        if (questionIndex >= this.quiz.getQuestions().size()) {
            this.state = SessionState.ARCHIVED;
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Žádné další otázky"
            );
        }

        QuestionResponse question = this.quiz.getQuestions().get(questionIndex);
        question.setSessionUuid(uuid);
        questionIndex++;

        if (questionIndex >= this.quiz.getQuestions().size()) {
            this.state = SessionState.ARCHIVED;
        }

        return question;
    }

    public ResultsResponse getResults() {
        if (state != SessionState.ARCHIVED) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Kvíz ještě nebyl dokončen"
            );
        }

        ResultsResponse response = new ResultsResponse();

        int score = 0;
        int maxScore = quiz.getQuestions().size();

        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            QuestionResponse question = quiz.getQuestions().get(i);

            List<Integer> correctAnswers = question.getCorrectAnswers();
            List<Integer> userAnswer =
                userAnswers.size() > i ? userAnswers.get(i) : new ArrayList<>();

            if (correctAnswers.equals(userAnswer)) {
                score++;
            }
        }

        response.setScore(score);
        response.setMaxScore(maxScore);
        response.setQuestions(quiz.getQuestions());
        response.setAnswers(userAnswers);

        return response;
    }
}
