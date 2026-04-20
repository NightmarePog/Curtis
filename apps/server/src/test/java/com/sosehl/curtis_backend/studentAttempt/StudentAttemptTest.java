package com.sosehl.curtis_backend.studentAttempt;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.session.SessionStatus;
import com.sosehl.curtis_backend.domain.v1.session.StudentAttempt;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentAttemptTest {

    private StudentAttempt attempt;

    @BeforeEach
    void setUp() {
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

        attempt = new StudentAttempt("student1", List.of(q1, q2));
    }

    @Test
    void shouldCalculateScoreCorrectly() {
        attempt.nextQuestion();
        attempt.addAnswer(List.of(0)); // správně
        attempt.nextQuestion();
        attempt.addAnswer(List.of(1)); // špatně

        assertThat(attempt.calculateScore()).isEqualTo(1);
    }

    @Test
    void shouldCalculateFullScore() {
        attempt.nextQuestion();
        attempt.addAnswer(List.of(0));
        attempt.nextQuestion();
        attempt.addAnswer(List.of(0));

        assertThat(attempt.calculateScore()).isEqualTo(2);
    }

    @Test
    void shouldCalculateZeroScore() {
        attempt.nextQuestion();
        attempt.addAnswer(List.of(1));
        attempt.nextQuestion();
        attempt.addAnswer(List.of(1));

        assertThat(attempt.calculateScore()).isEqualTo(0);
    }

    @Test
    void shouldFinishAttempt() {
        attempt.finish();
        assertThat(attempt.getStatus()).isEqualTo(SessionStatus.ARCHIVED);
    }

    @Test
    void shouldThrowAfterFinish() {
        attempt.finish();
        assertThatThrownBy(() -> attempt.nextQuestion()).isInstanceOf(
            IllegalStateException.class
        );
    }
}
