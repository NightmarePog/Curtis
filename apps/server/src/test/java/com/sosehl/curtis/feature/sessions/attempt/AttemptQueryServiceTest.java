package com.sosehl.curtis.feature.sessions.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.GradingState;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttemptQueryServiceTest {

    private final QuizSessionRepository sessions = mock(
        QuizSessionRepository.class
    );
    private final AttemptRepository attempts = mock(AttemptRepository.class);
    private final AttemptQuestionRepository questions = mock(
        AttemptQuestionRepository.class
    );
    private final UserDirectory users = mock(UserDirectory.class);
    private final SessionChangeNotifier changes = mock(
        SessionChangeNotifier.class
    );
    private final ObjectMapper mapper = new ObjectMapper();
    private final Instant now = Instant.parse("2026-09-02T12:00:00Z");
    private final UUID sessionId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private AttemptQueryService service;
    private Attempt attempt;
    private AttemptQuestion question;
    private QuizSession session;

    @BeforeEach
    void setUp() {
        service = new AttemptQueryService(
            sessions,
            attempts,
            questions,
            users,
            mapper,
            changes,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        attempt = new Attempt(
            attemptId,
            sessionId,
            studentId,
            1,
            now.minusSeconds(60),
            3
        );
        question = new AttemptQuestion(
            questionId,
            attemptId,
            0,
            UUID.randomUUID(),
            textSnapshot("Answered prompt"),
            3
        );
        session = activeSession();
        when(attempts.findLockedById(attemptId)).thenReturn(Optional.of(attempt));
        when(attempts.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(questions.findById(questionId)).thenReturn(Optional.of(question));
        when(questions.findLockedById(questionId)).thenReturn(Optional.of(question));
        when(questions.findByAttemptIdOrderByPosition(attemptId))
            .thenReturn(List.of(question));
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        when(users.summariesByIds(any())).thenReturn(List.of());
    }

    @Test
    void cannotGradeAnAnswerBeforeItsAttemptIsSubmitted() {
        answerPendingQuestion();

        assertThatThrownBy(() ->
            service.gradeForTeacher(questionId, 2, teacherId)
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo(
                    "review.attempt_in_progress"
                )
            );
        assertThat(question.getGradingState()).isEqualTo(
            GradingState.PENDING_REVIEW
        );
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.IN_PROGRESS);
    }

    @Test
    void cannotGradeAQuestionThatIsNotPendingReview() {
        assertThatThrownBy(() ->
            service.gradeForTeacher(questionId, 2, teacherId)
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("review.not_pending")
            );
    }

    @Test
    void gradingSubmittedPendingAnswerFinalizesAttempt() {
        answerPendingQuestion();
        attempt.finalizeAttempt(AttemptStatus.PENDING_REVIEW, 0, 1, now);

        service.gradeForTeacher(questionId, 2, teacherId);

        assertThat(question.getGradingState()).isEqualTo(
            GradingState.MANUALLY_GRADED
        );
        assertThat(question.getAwardedPoints()).isEqualTo(2);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.GRADED);
        assertThat(attempt.getScore()).isEqualTo(2);
        assertThat(attempt.getPendingReviewCount()).isZero();
    }

    @Test
    void activeAttemptDetailIsNotAvailableToStudent() {
        assertThatThrownBy(() ->
            service.detailForStudent(attemptId, studentId)
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("result.not_available")
            );
    }

    @Test
    void activeSessionResultIsUnavailableEvenAfterSubmission() {
        answerPendingQuestion();
        attempt.finalizeAttempt(AttemptStatus.PENDING_REVIEW, 0, 1, now);

        assertThatThrownBy(() ->
            service.detailForStudent(attemptId, studentId)
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("result.not_available")
            );
    }

    @Test
    void activeSessionAttemptsAreNotListedAsResultHistory() {
        when(sessions.findByStatusAndClosesAtAfter(
            com.sosehl.curtis.feature.sessions.core.SessionStatus.ACTIVE,
            now
        )).thenReturn(List.of(session));
        when(attempts.findByStudentIdAndStatusNotAndSessionIdNotInOrderByStartedAtDesc(
            org.mockito.ArgumentMatchers.eq(studentId),
            org.mockito.ArgumentMatchers.eq(AttemptStatus.IN_PROGRESS),
            org.mockito.ArgumentMatchers.eq(List.of(sessionId)),
            any()
        )).thenReturn(List.of());

        assertThat(service.listForStudent(studentId, 100)).isEmpty();
        verify(attempts).findByStudentIdAndStatusNotAndSessionIdNotInOrderByStartedAtDesc(
            org.mockito.ArgumentMatchers.eq(studentId),
            org.mockito.ArgumentMatchers.eq(AttemptStatus.IN_PROGRESS),
            org.mockito.ArgumentMatchers.eq(List.of(sessionId)),
            any()
        );
    }

    private void answerPendingQuestion() {
        question.serve(now.minusSeconds(20), now.plusSeconds(10));
        ObjectNode response = mapper.createObjectNode();
        response.put("type", QuestionType.FREE_TEXT.name());
        response.put("text", "Student answer");
        question.answer(response, null, GradingState.PENDING_REVIEW, now);
    }

    private ObjectNode textSnapshot(String prompt) {
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("type", QuestionType.FREE_TEXT.name());
        snapshot.put("prompt", prompt);
        snapshot.put("points", 3);
        snapshot.putArray("options");
        snapshot.putArray("pairs");
        return snapshot;
    }

    private QuizSession activeSession() {
        return new QuizSession(
            sessionId,
            UUID.randomUUID(),
            teacherId,
            "Quiz",
            null,
            "Subject",
            null,
            "Teacher",
            mapper.createObjectNode(),
            now.minusSeconds(300),
            now.plusSeconds(300),
            2,
            ScorePolicy.BEST
        );
    }
}
