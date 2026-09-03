package com.sosehl.curtis.feature.sessions.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sosehl.curtis.feature.quizzes.core.OptionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuestionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.SessionParticipantRepository;
import com.sosehl.curtis.feature.sessions.core.AttemptQuestionState;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttemptServiceTest {

    private final QuizSessionRepository sessions = mock(QuizSessionRepository.class);
    private final SessionParticipantRepository participants = mock(
        SessionParticipantRepository.class
    );
    private final AttemptRepository attempts = mock(AttemptRepository.class);
    private final AttemptQuestionRepository questions = mock(
        AttemptQuestionRepository.class
    );
    private final SessionChangeNotifier changes = mock(
        SessionChangeNotifier.class
    );
    private final ObjectMapper mapper = new ObjectMapper();
    private final Instant now = Instant.parse("2026-09-02T12:00:00Z");
    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID firstOption = UUID.fromString(
        "ffffffff-ffff-ffff-ffff-ffffffffffff"
    );
    private final UUID secondOption = UUID.fromString(
        "00000000-0000-0000-0000-000000000001"
    );
    private AttemptService service;
    private Attempt attempt;
    private AttemptQuestion question;

    @BeforeEach
    void setUp() {
        service = new AttemptService(
            sessions,
            participants,
            attempts,
            questions,
            mapper,
            changes,
            Clock.fixed(now, ZoneOffset.UTC)
        );
        attempt = new Attempt(attemptId, sessionId, studentId, 1, now, 2);
        question = new AttemptQuestion(
            questionId,
            attemptId,
            0,
            UUID.randomUUID(),
            choiceSnapshot(),
            2
        );
        question.serve(now.minusSeconds(5), now.plusSeconds(30));
        when(attempts.findLockedById(attemptId)).thenReturn(Optional.of(attempt));
        when(attempts.findByIdAndStudentId(attemptId, studentId))
            .thenReturn(Optional.of(attempt));
        when(questions.findLockedById(questionId)).thenReturn(Optional.of(question));
        when(questions.findByAttemptIdAndPosition(attemptId, 1))
            .thenReturn(Optional.empty());
        when(questions.findByAttemptIdOrderByPosition(attemptId))
            .thenReturn(List.of(question));
        QuizSession session = activeSession();
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessions.findLockedById(sessionId)).thenReturn(Optional.of(session));
    }

    @Test
    void reorderedEquivalentChoiceRetryIsIdempotentAfterAdvance() {
        AnswerCommand first = new AnswerCommand(
            QuestionType.MULTIPLE_CHOICE,
            List.of(firstOption, secondOption),
            List.of(),
            null
        );
        AnswerCommand retry = new AnswerCommand(
            QuestionType.MULTIPLE_CHOICE,
            List.of(secondOption, firstOption),
            List.of(),
            null
        );

        service.answer(attemptId, questionId, first, studentId);
        service.answer(attemptId, questionId, retry, studentId);

        assertThat(attempt.getCurrentPosition()).isEqualTo(1);
        assertThat(question.getState()).isEqualTo(AttemptQuestionState.ANSWERED);
        assertThat(question.getResponse().path("optionIds").get(0).asText())
            .isEqualTo(secondOption.toString());
    }

    @Test
    void differentRetryCannotReplaceLockedAnswer() {
        service.answer(
            attemptId,
            questionId,
            new AnswerCommand(
                QuestionType.MULTIPLE_CHOICE,
                List.of(firstOption, secondOption),
                List.of(),
                null
            ),
            studentId
        );

        assertThatThrownBy(() ->
            service.answer(
                attemptId,
                questionId,
                new AnswerCommand(
                    QuestionType.MULTIPLE_CHOICE,
                    List.of(firstOption),
                    List.of(),
                    null
                ),
                studentId
            )
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("attempt.answer_locked")
            );
    }

    @Test
    void deadlineIsExclusiveAndCannotBeExtendedByRetry() {
        question = new AttemptQuestion(
            questionId,
            attemptId,
            0,
            UUID.randomUUID(),
            choiceSnapshot(),
            2
        );
        question.serve(now.minusSeconds(30), now);
        when(questions.findLockedById(questionId)).thenReturn(Optional.of(question));
        when(questions.findByAttemptIdOrderByPosition(attemptId))
            .thenReturn(List.of(question));

        service.answer(
            attemptId,
            questionId,
            new AnswerCommand(
                QuestionType.MULTIPLE_CHOICE,
                List.of(firstOption, secondOption),
                List.of(),
                null
            ),
            studentId
        );

        assertThat(question.getState()).isEqualTo(AttemptQuestionState.TIMED_OUT);
        assertThat(question.getAwardedPoints()).isZero();
        assertThat(attempt.getCurrentPosition()).isEqualTo(1);
    }

    @Test
    void matchingAnswerRejectsReuseOfRightItem() {
        UUID leftOne = UUID.randomUUID();
        UUID leftTwo = UUID.randomUUID();
        UUID rightOne = UUID.randomUUID();
        UUID rightTwo = UUID.randomUUID();
        question = new AttemptQuestion(
            questionId,
            attemptId,
            0,
            UUID.randomUUID(),
            matchingSnapshot(leftOne, leftTwo, rightOne, rightTwo),
            2
        );
        question.serve(now.minusSeconds(1), now.plusSeconds(30));
        when(questions.findLockedById(questionId)).thenReturn(Optional.of(question));

        assertThatThrownBy(() ->
            service.answer(
                attemptId,
                questionId,
                new AnswerCommand(
                    QuestionType.MATCHING,
                    List.of(),
                    List.of(
                        new AnswerCommand.Match(leftOne, rightOne),
                        new AnswerCommand.Match(leftTwo, rightOne)
                    ),
                    null
                ),
                studentId
            )
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("answer.duplicate_match")
            );
        assertThat(attempt.getCurrentPosition()).isZero();
    }

    @Test
    void answerRejectsFieldsFromAnotherQuestionType() {
        assertThatThrownBy(() ->
            service.answer(
                attemptId,
                questionId,
                new AnswerCommand(
                    QuestionType.MULTIPLE_CHOICE,
                    List.of(firstOption, secondOption),
                    List.of(),
                    "ignored payload"
                ),
                studentId
            )
        )
            .isInstanceOfSatisfying(ProblemException.class, exception ->
                assertThat(exception.code()).isEqualTo("answer.invalid_shape")
            );
        assertThat(question.getResponse()).isNull();
        assertThat(attempt.getCurrentPosition()).isZero();
    }

    @Test
    void answerAtSessionDeadlineExpiresWithoutScoring() {
        QuizSession expired = sessionEndingAt(now);
        when(sessions.findLockedById(sessionId)).thenReturn(Optional.of(expired));

        var view = service.answer(
            attemptId,
            questionId,
            new AnswerCommand(
                QuestionType.MULTIPLE_CHOICE,
                List.of(firstOption, secondOption),
                List.of(),
                null
            ),
            studentId
        );

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.EXPIRED);
        assertThat(question.getState()).isEqualTo(AttemptQuestionState.TIMED_OUT);
        assertThat(question.getAwardedPoints()).isZero();
        assertThat(view.score()).isZero();
    }

    @Test
    void submittedScoreStaysHiddenUntilSessionCloses() {
        service.answer(
            attemptId,
            questionId,
            new AnswerCommand(
                QuestionType.MULTIPLE_CHOICE,
                List.of(firstOption, secondOption),
                List.of(),
                null
            ),
            studentId
        );

        var view = service.submit(attemptId, studentId);

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.GRADED);
        assertThat(attempt.getScore()).isEqualTo(2);
        assertThat(view.score()).isNull();
    }

    @Test
    void questionDeadlineNeverExtendsPastSessionDeadline() {
        Instant closesAt = now.plusSeconds(10);
        QuizSession shortSession = sessionEndingAt(closesAt);
        question = new AttemptQuestion(
            questionId,
            attemptId,
            0,
            UUID.randomUUID(),
            choiceSnapshot(),
            2
        );
        when(sessions.findLockedById(sessionId)).thenReturn(Optional.of(shortSession));
        when(questions.findByAttemptIdAndPosition(attemptId, 0))
            .thenReturn(Optional.of(question));
        when(questions.findByAttemptIdOrderByPosition(attemptId))
            .thenReturn(List.of(question));

        var view = service.resume(attemptId, studentId);

        assertThat(view.currentQuestion().deadlineAt()).isEqualTo(closesAt);
    }

    private JsonNode choiceSnapshot() {
        return mapper.valueToTree(
            new QuestionSnapshot(
                UUID.randomUUID(),
                0,
                QuestionType.MULTIPLE_CHOICE,
                "Select both",
                2,
                null,
                null,
                30,
                List.of(
                    new OptionSnapshot(firstOption, 0, "First", true),
                    new OptionSnapshot(secondOption, 1, "Second", true)
                ),
                List.of()
            )
        );
    }

    private JsonNode matchingSnapshot(
        UUID leftOne,
        UUID leftTwo,
        UUID rightOne,
        UUID rightTwo
    ) {
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("type", QuestionType.MATCHING.name());
        snapshot.put("points", 2);
        ArrayNode pairs = snapshot.putArray("pairs");
        addPair(pairs, leftOne, rightOne, "A", "1");
        addPair(pairs, leftTwo, rightTwo, "B", "2");
        return snapshot;
    }

    private void addPair(
        ArrayNode pairs,
        UUID left,
        UUID right,
        String leftText,
        String rightText
    ) {
        ObjectNode pair = pairs.addObject();
        pair.put("pairId", left.toString());
        pair.put("rightItemId", right.toString());
        pair.put("left", leftText);
        pair.put("right", rightText);
    }

    private QuizSession activeSession() {
        return sessionEndingAt(now.plusSeconds(3600));
    }

    private QuizSession sessionEndingAt(Instant closesAt) {
        return new QuizSession(
            sessionId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Quiz",
            null,
            "Subject",
            null,
            "Teacher",
            mapper.createObjectNode(),
            now.minusSeconds(60),
            closesAt,
            1,
            ScorePolicy.BEST
        );
    }
}
