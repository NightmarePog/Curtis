package com.sosehl.curtis_backend.session;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.session.Session;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.NoMoreQuestionsException;
import com.sosehl.curtis_backend.domain.v1.session.exceptions.SessionExpiredException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SessionTest {

    private static final Logger log = LoggerFactory.getLogger(
        SessionTest.class
    );

    private QuizGetResponse quiz;

    @BeforeEach
    void setUp() {
        log.info("Setting up test quiz data...");

        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Java");
        correct.setIsCorrect(true);

        QuestionAnswer wrong = new QuestionAnswer();
        wrong.setAnswer("Python");
        wrong.setIsCorrect(false);

        QuestionResponse q1 = new QuestionResponse();
        q1.setQuestion("Co je Java?");
        q1.setAnswers(List.of(correct, wrong));

        QuestionResponse q2 = new QuestionResponse();
        q2.setQuestion("Co je Spring?");
        q2.setAnswers(List.of(correct, wrong));

        quiz = new QuizGetResponse();
        quiz.setTitle("Test kvíz");
        quiz.setQuestions(List.of(q1, q2));
        quiz.setShuffle(false);
    }

    @Test
    void shouldCreateSessionSuccessfully() {
        log.info("Testing session creation...");
        Session session = new Session("admin123", quiz, 30);
        assertThat(session.getUuid())
            .withFailMessage("UUID should not be null for a new session")
            .isNotNull();
        assertThat(session.getAdminId())
            .withFailMessage("Admin ID should match the one provided")
            .isEqualTo("admin123");
    }

    @Test
    void shouldThrowWhenQuizIsNull() {
        log.info("Testing session creation with null quiz...");
        assertThatThrownBy(() -> new Session("admin123", null, 30))
            .withFailMessage(
                "Creating a session with null quiz should throw IllegalArgumentException"
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("quiz is missing");
    }

    @Test
    void shouldThrowWhenAdminIdIsNull() {
        log.info("Testing session creation with null admin ID...");
        assertThatThrownBy(() -> new Session(null, quiz, 30))
            .withFailMessage(
                "Creating a session with null adminId should throw IllegalArgumentException"
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("adminId is missing");
    }

    @Test
    void shouldNotAllowStudentToJoinTwice() {
        log.info("Testing student cannot join twice...");
        Session session = new Session("admin123", quiz, 30);
        session.join("student1");

        assertThatThrownBy(() -> session.join("student1"))
            .withFailMessage(
                "Joining the same student twice should throw IllegalStateException"
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Student již session hrál");
    }

    @Test
    void shouldReturnNextQuestion() {
        log.info("Testing getting the next question...");
        Session session = new Session("admin123", quiz, 30);
        session.join("student1");

        QuestionResponse q = session.nextQuestion("student1");
        assertThat(q)
            .withFailMessage("Next question should not be null")
            .isNotNull();
        assertThat(q.getQuestion())
            .withFailMessage("Question text should not be blank")
            .isNotBlank();
    }

    @Test
    void shouldThrowWhenNoMoreQuestions() {
        Session session = new Session("admin123", quiz, 30);
        session.join("student1");

        // student vyčerpá otázky
        session.nextQuestion("student1"); // 1. otázka
        session.nextQuestion("student1"); // 2. otázka

        assertThatThrownBy(() -> session.nextQuestion("student1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Pokus již byl ukončen");
    }

    @Test
    void shouldThrowWhenSessionExpired() {
        log.info("Testing behavior when session is expired...");
        Session session = new Session("admin123", quiz, -1);

        assertThatThrownBy(() -> session.join("student1"))
            .withFailMessage("Session vypršela při pokusu o připojení")
            .isInstanceOf(SessionExpiredException.class);
    }
}
