package com.sosehl.curtis_backend.studentAttempt;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis_backend.domain.v1.question.QuestionAnswer;
import com.sosehl.curtis_backend.domain.v1.question.MatchingPair;
import com.sosehl.curtis_backend.domain.v1.question.QuestionType;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.session.SessionStatus;
import com.sosehl.curtis_backend.domain.v1.session.StudentAttempt;
import com.sosehl.curtis_backend.domain.v1.session.MatchingSubmissionPair;
import com.sosehl.curtis_backend.domain.v1.session.QuestionSubmission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentAttemptTest {

    private StudentAttempt attempt;

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private QuestionResponse timedQuestion(int timeInSeconds) {
        QuestionAnswer correct = new QuestionAnswer();
        correct.setAnswer("Java");
        correct.setIsCorrect(true);

        QuestionAnswer wrong = new QuestionAnswer();
        wrong.setAnswer("Python");
        wrong.setIsCorrect(false);

        QuestionResponse question = new QuestionResponse();
        question.setQuestion("Co je Java?");
        question.setAnswers(List.of(correct, wrong));
        question.setTimeInSeconds(timeInSeconds);
        return question;
    }

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

    @Test
    void shouldRecordAnswerWithinTimeLimit() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(5));
        timedAttempt.addAnswer(List.of(0));

        assertThat(timedAttempt.calculateScore()).isEqualTo(1);
    }

    @Test
    void shouldDiscardAnswerSubmittedAfterGracePeriod() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(13)); // 10s limit + 2s grace = 12s allowed
        timedAttempt.addAnswer(List.of(0)); // correct answer, but too late

        assertThat(timedAttempt.calculateScore()).isEqualTo(0);
    }

    @Test
    void shouldAcceptAnswerExactlyAtGraceBoundary() {
        MutableClock clock = new MutableClock(Instant.parse("2024-01-01T00:00:00Z"));
        StudentAttempt timedAttempt = new StudentAttempt(
            "student1",
            List.of(timedQuestion(10)),
            clock
        );

        timedAttempt.nextQuestion();
        clock.advance(Duration.ofSeconds(12)); // exactly 10s + 2s grace
        timedAttempt.addAnswer(List.of(0));

        assertThat(timedAttempt.calculateScore()).isEqualTo(1);
    }

    @Test
    void shouldUseConfiguredPointsForCorrectMultipleChoiceAnswers() {
        QuestionResponse question = timedQuestion(10);
        question.setPoints(3);
        StudentAttempt weightedAttempt = new StudentAttempt(
            "student1",
            List.of(question)
        );

        weightedAttempt.nextQuestion();
        weightedAttempt.addAnswer(List.of(0));

        assertThat(weightedAttempt.calculateScore()).isEqualTo(3);
    }

    @Test
    void shouldNotAutoScoreQuestionsRequiringManualGrading() {
        QuestionResponse question = new QuestionResponse();
        question.setQuestion("Explain the answer");
        question.setType(QuestionType.FREE_TEXT);
        question.setPoints(3);
        StudentAttempt freeTextAttempt = new StudentAttempt(
            "student1",
            List.of(question)
        );

        freeTextAttempt.nextQuestion();
        freeTextAttempt.addAnswer(List.of());

        assertThat(freeTextAttempt.calculateScore()).isZero();
    }

    @Test
    void shouldScoreMatchingOnlyWhenEveryPairMatches() {
        QuestionResponse question = new QuestionResponse();
        question.setType(QuestionType.MATCHING);
        question.setPoints(4);
        question.setPairs(
            List.of(new MatchingPair("one", "1"), new MatchingPair("two", "2"))
        );
        StudentAttempt matchingAttempt = new StudentAttempt(
            "student1",
            List.of(question)
        );

        matchingAttempt.nextQuestion();
        QuestionSubmission submission = new QuestionSubmission();
        submission.setType(QuestionType.MATCHING);
        submission.setPairs(
            List.of(
                new MatchingSubmissionPair(0, 0),
                new MatchingSubmissionPair(1, 1)
            )
        );
        matchingAttempt.addSubmission(submission);

        assertThat(matchingAttempt.calculateScore()).isEqualTo(4);
    }

    @Test
    void shouldRejectMatchingSubmissionWithDuplicateIndexes() {
        QuestionResponse question = new QuestionResponse();
        question.setType(QuestionType.MATCHING);
        question.setPairs(
            List.of(new MatchingPair("one", "1"), new MatchingPair("two", "2"))
        );
        StudentAttempt matchingAttempt = new StudentAttempt(
            "student1",
            List.of(question)
        );
        matchingAttempt.nextQuestion();
        QuestionSubmission submission = new QuestionSubmission();
        submission.setType(QuestionType.MATCHING);
        submission.setPairs(
            List.of(
                new MatchingSubmissionPair(0, 0),
                new MatchingSubmissionPair(0, 1)
            )
        );

        assertThatThrownBy(() -> matchingAttempt.addSubmission(submission))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
