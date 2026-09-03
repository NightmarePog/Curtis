package com.sosehl.curtis.feature.sessions.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sosehl.curtis.feature.quizzes.core.QuestionSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.SessionParticipantId;
import com.sosehl.curtis.feature.sessions.SessionParticipantRepository;
import com.sosehl.curtis.feature.sessions.core.AttemptQuestionState;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.GradingState;
import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.MatchingItemResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.QuestionOptionResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.StudentQuestionResponse;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttemptService {
    private final QuizSessionRepository sessions;
    private final SessionParticipantRepository participants;
    private final AttemptRepository attempts;
    private final AttemptQuestionRepository questions;
    private final ObjectMapper objectMapper;
    private final SessionChangeNotifier changes;
    private final Clock clock;

    public AttemptService(
        QuizSessionRepository sessions,
        SessionParticipantRepository participants,
        AttemptRepository attempts,
        AttemptQuestionRepository questions,
        ObjectMapper objectMapper,
        SessionChangeNotifier changes,
        Clock clock
    ) {
        this.sessions = sessions;
        this.participants = participants;
        this.attempts = attempts;
        this.questions = questions;
        this.objectMapper = objectMapper;
        this.changes = changes;
        this.clock = clock;
    }

    @Transactional
    public AttemptResponse startOrResume(UUID sessionId, UUID studentId) {
        QuizSession session = sessions.findLockedById(sessionId).orElseThrow(() ->
            ProblemException.notFound("session.not_found", "The session was not found.")
        );
        Instant now = clock.instant();
        if (!session.isActiveAt(now)) {
            throw ProblemException.conflict("session.closed", "The session is no longer active.");
        }
        if (!participants.existsById(new SessionParticipantId(sessionId, studentId))) {
            throw ProblemException.notFound("session.not_found", "The session was not found.");
        }

        Attempt current = attempts
            .findFirstBySessionIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
                sessionId, studentId, AttemptStatus.IN_PROGRESS
            )
            .orElse(null);
        if (current != null) return resumeLocked(current, session, now);

        int used = attempts.countBySessionIdAndStudentId(sessionId, studentId);
        if (used >= session.getMaxAttempts()) {
            throw ProblemException.conflict(
                "attempt.limit_reached",
                "All attempts allowed for this session have already been used."
            );
        }

        List<QuestionSnapshot> selected = readQuestions(session.getQuizSnapshot());
        if (session.getQuizSnapshot().path("shuffle").asBoolean(false)) {
            Collections.shuffle(selected);
        }
        int limit = Math.min(
            selected.size(),
            session.getQuizSnapshot().path("maxQuestionsPerSession").asInt(selected.size())
        );
        selected = new ArrayList<>(selected.subList(0, limit));
        int maxScore = selected.stream().mapToInt(QuestionSnapshot::points).sum();
        Attempt attempt = new Attempt(
            UUID.randomUUID(), sessionId, studentId, used + 1, now, maxScore
        );
        attempts.saveAndFlush(attempt);

        List<AttemptQuestion> rows = new ArrayList<>();
        for (int index = 0; index < selected.size(); index++) {
            QuestionSnapshot source = selected.get(index);
            rows.add(new AttemptQuestion(
                UUID.randomUUID(), attempt.getId(), index, source.questionId(),
                prepareQuestionSnapshot(source), source.points()
            ));
        }
        questions.saveAll(rows);
        return resumeLocked(attempt, session, now);
    }

    @Transactional
    public AttemptResponse resume(UUID attemptId, UUID studentId) {
        LockedAttempt locked = lockOwnedWithSession(attemptId, studentId);
        return resumeLocked(locked.attempt(), locked.session(), clock.instant());
    }

    @Transactional
    public AttemptResponse answer(
        UUID attemptId,
        UUID attemptQuestionId,
        AnswerCommand request,
        UUID studentId
    ) {
        LockedAttempt locked = lockOwnedWithSession(attemptId, studentId);
        Attempt attempt = locked.attempt();
        QuizSession session = locked.session();
        Instant now = clock.instant();
        if (
            attempt.getStatus() == AttemptStatus.IN_PROGRESS &&
            !session.isActiveAt(now)
        ) {
            finalizeAttempt(attempt, now, AttemptStatus.EXPIRED);
            publishResultChange(attempt, session);
            return toResponse(attempt, null, session, now);
        }
        AttemptQuestion question = questions.findLockedById(attemptQuestionId).orElseThrow(() ->
            ProblemException.notFound("attempt.question_not_found", "The attempt question was not found.")
        );
        if (!question.getAttemptId().equals(attemptId)) {
            throw ProblemException.notFound(
                "attempt.question_not_found",
                "The attempt question was not found."
            );
        }
        JsonNode serialized = canonicalAnswer(request);
        if (question.getState() == AttemptQuestionState.ANSWERED) {
            if (!serialized.equals(question.getResponse())) {
                throw ProblemException.conflict("attempt.answer_locked", "The answer has already been locked.");
            }
            return resumeLocked(attempt, session, now);
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw ProblemException.conflict("attempt.finished", "The attempt has already finished.");
        }
        if (question.getPosition() != attempt.getCurrentPosition()) {
            throw ProblemException.conflict(
                "attempt.question_out_of_order",
                "The answer does not belong to the current question."
            );
        }
        if (question.getState() != AttemptQuestionState.SERVED) {
            throw ProblemException.conflict("attempt.question_not_active", "The question is not currently open.");
        }

        if (question.getDeadlineAt() != null && !now.isBefore(question.getDeadlineAt())) {
            question.timeOut(now);
        } else {
            Grade grade = validateAndGrade(question.getQuestionSnapshot(), request);
            question.answer(serialized, grade.points(), grade.state(), now);
        }
        attempt.advance();
        return resumeLocked(attempt, session, now);
    }

    @Transactional
    public AttemptResponse submit(UUID attemptId, UUID studentId) {
        LockedAttempt locked = lockOwnedWithSession(attemptId, studentId);
        Attempt attempt = locked.attempt();
        QuizSession session = locked.session();
        Instant now = clock.instant();
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return toResponse(attempt, null, session, now);
        }
        AttemptStatus terminal = session.isActiveAt(now)
            ? AttemptStatus.SUBMITTED
            : AttemptStatus.EXPIRED;
        finalizeAttempt(attempt, now, terminal);
        publishResultChange(attempt, session);
        return toResponse(attempt, null, session, now);
    }

    @Transactional
    public void hardStopSession(UUID sessionId, Instant now) {
        QuizSession session = sessions.findById(sessionId).orElseThrow(() ->
            ProblemException.notFound("session.not_found", "The session was not found.")
        );
        for (Attempt attempt : attempts.findBySessionIdAndStatus(sessionId, AttemptStatus.IN_PROGRESS)) {
            finalizeAttempt(attempt, now, AttemptStatus.EXPIRED);
            publishResultChange(attempt, session);
        }
    }

    private AttemptResponse resumeLocked(
        Attempt attempt,
        QuizSession session,
        Instant now
    ) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return toResponse(attempt, null, session, now);
        }
        if (!session.isActiveAt(now)) {
            finalizeAttempt(attempt, now, AttemptStatus.EXPIRED);
            publishResultChange(attempt, session);
            return toResponse(attempt, null, session, now);
        }

        while (true) {
            AttemptQuestion current = questions
                .findByAttemptIdAndPosition(attempt.getId(), attempt.getCurrentPosition())
                .orElse(null);
            if (current == null) return toResponse(attempt, null, session, now);
            if (current.getState() == AttemptQuestionState.SERVED
                && current.getDeadlineAt() != null
                && !now.isBefore(current.getDeadlineAt())) {
                current.timeOut(now);
                attempt.advance();
                continue;
            }
            if (current.getState() == AttemptQuestionState.READY) {
                int seconds = Math.max(1, current.getQuestionSnapshot().path("timeSeconds").asInt(30));
                Instant questionDeadline = now.plusSeconds(seconds);
                if (questionDeadline.isAfter(session.getClosesAt())) {
                    questionDeadline = session.getClosesAt();
                }
                current.serve(now, questionDeadline);
            }
            return toResponse(attempt, current, session, now);
        }
    }

    private LockedAttempt lockOwnedWithSession(
        UUID attemptId,
        UUID studentId
    ) {
        UUID sessionId = attempts
            .findByIdAndStudentId(attemptId, studentId)
            .map(Attempt::getSessionId)
            .orElseThrow(() ->
                ProblemException.notFound(
                    "attempt.not_found",
                    "The attempt was not found."
                )
            );
        QuizSession session = sessions.findLockedById(sessionId).orElseThrow(() ->
            ProblemException.notFound(
                "session.not_found",
                "The session was not found."
            )
        );
        Attempt attempt = ownedLocked(attemptId, studentId);
        if (!attempt.getSessionId().equals(sessionId)) {
            throw ProblemException.notFound(
                "attempt.not_found",
                "The attempt was not found."
            );
        }
        return new LockedAttempt(attempt, session);
    }

    private Attempt ownedLocked(UUID attemptId, UUID studentId) {
        Attempt attempt = attempts.findLockedById(attemptId).orElseThrow(() ->
            ProblemException.notFound("attempt.not_found", "The attempt was not found.")
        );
        if (!attempt.getStudentId().equals(studentId)) {
            throw ProblemException.notFound("attempt.not_found", "The attempt was not found.");
        }
        return attempt;
    }

    private void finalizeAttempt(Attempt attempt, Instant now, AttemptStatus emptyTerminal) {
        List<AttemptQuestion> rows = questions.findByAttemptIdOrderByPosition(attempt.getId());
        rows.stream()
            .filter(value -> value.getState() == AttemptQuestionState.READY
                || value.getState() == AttemptQuestionState.SERVED)
            .forEach(value -> value.timeOut(now));
        int pending = (int) rows.stream()
            .filter(value -> value.getGradingState() == GradingState.PENDING_REVIEW)
            .count();
        int score = rows.stream()
            .map(AttemptQuestion::getAwardedPoints)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
        AttemptStatus terminal = pending > 0
            ? AttemptStatus.PENDING_REVIEW
            : (emptyTerminal == AttemptStatus.EXPIRED ? AttemptStatus.EXPIRED : AttemptStatus.GRADED);
        attempt.finalizeAttempt(terminal, score, pending, now);
    }

    private Grade validateAndGrade(JsonNode snapshot, AnswerCommand request) {
        QuestionType expected;
        try {
            expected = QuestionType.valueOf(snapshot.path("type").asText());
        } catch (IllegalArgumentException exception) {
            throw ProblemException.badRequest("answer.invalid_type", "The question type is not supported.");
        }
        if (request.type() != expected) {
            throw ProblemException.badRequest("answer.type_mismatch", "The answer type does not match the question.");
        }
        validateAnswerShape(request);
        int points = snapshot.path("points").asInt(1);
        return switch (expected) {
            case MULTIPLE_CHOICE -> gradeChoice(snapshot, request.optionIds(), points);
            case MATCHING -> gradeMatching(snapshot, request.pairs(), points);
            case FREE_TEXT -> gradeText(request.text(), points);
        };
    }

    private Grade gradeChoice(JsonNode snapshot, List<UUID> selected, int points) {
        if (new HashSet<>(selected).size() != selected.size()) {
            throw ProblemException.badRequest("answer.duplicate_option", "An option cannot be selected more than once.");
        }
        Set<UUID> allowed = new HashSet<>();
        Set<UUID> correct = new HashSet<>();
        snapshot.path("options").forEach(option -> {
            UUID id = UUID.fromString(option.path("optionId").asText());
            allowed.add(id);
            if (option.path("correct").asBoolean()) correct.add(id);
        });
        if (!allowed.containsAll(selected)) {
            throw ProblemException.badRequest("answer.unknown_option", "The answer contains an unknown option.");
        }
        return new Grade(correct.equals(new HashSet<>(selected)) ? points : 0, GradingState.AUTO_GRADED);
    }

    private Grade gradeMatching(
        JsonNode snapshot,
        List<AnswerCommand.Match> submitted,
        int points
    ) {
        Map<UUID, UUID> expected = new HashMap<>();
        snapshot.path("pairs").forEach(pair -> expected.put(
            UUID.fromString(pair.path("pairId").asText()),
            UUID.fromString(pair.path("rightItemId").asText())
        ));
        Map<UUID, UUID> actual = new HashMap<>();
        Set<UUID> usedRightItems = new HashSet<>();
        for (AnswerCommand.Match pair : submitted) {
            if (actual.put(pair.leftId(), pair.rightId()) != null) {
                throw ProblemException.badRequest("answer.duplicate_pair", "Each left-side item may be used only once.");
            }
            if (!usedRightItems.add(pair.rightId())) {
                throw ProblemException.badRequest(
                    "answer.duplicate_match",
                    "Each right-side item may be used only once."
                );
            }
        }
        if (!expected.keySet().equals(actual.keySet())
            || !new HashSet<>(expected.values()).containsAll(actual.values())) {
            throw ProblemException.badRequest("answer.invalid_pairs", "The matching answer is incomplete or contains an unknown item.");
        }
        return new Grade(expected.equals(actual) ? points : 0, GradingState.AUTO_GRADED);
    }

    private Grade gradeText(String text, int points) {
        if (text == null) {
            throw ProblemException.badRequest("answer.text_required", "A text answer is required.");
        }
        if (text.length() > 10000) {
            throw ProblemException.badRequest("answer.text_too_long", "The text answer is too long.");
        }
        return text.isBlank()
            ? new Grade(0, GradingState.AUTO_GRADED)
            : new Grade(null, GradingState.PENDING_REVIEW);
    }

    private List<QuestionSnapshot> readQuestions(JsonNode quizSnapshot) {
        List<QuestionSnapshot> values = new ArrayList<>();
        quizSnapshot.path("questions").forEach(node ->
            values.add(objectMapper.convertValue(node, QuestionSnapshot.class))
        );
        if (values.isEmpty()) {
            throw ProblemException.conflict("quiz.empty", "The quiz does not contain any questions.");
        }
        return values;
    }

    private JsonNode prepareQuestionSnapshot(QuestionSnapshot source) {
        ObjectNode node = objectMapper.valueToTree(source);
        if (source.type() == QuestionType.MATCHING) {
            ArrayNode pairs = (ArrayNode) node.withArray("pairs");
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < pairs.size(); i++) order.add(i);
            Collections.shuffle(order);
            for (int i = 0; i < pairs.size(); i++) {
                ObjectNode pair = (ObjectNode) pairs.get(i);
                UUID pairId = UUID.fromString(pair.path("pairId").asText());
                pair.put("rightItemId", UUID.nameUUIDFromBytes(
                    (UUID.randomUUID() + ":" + pairId).getBytes(StandardCharsets.UTF_8)
                ).toString());
                pair.put("rightOrder", order.indexOf(i));
            }
        }
        return node;
    }

    private JsonNode canonicalAnswer(AnswerCommand request) {
        validateAnswerShape(request);
        ObjectNode value = objectMapper.createObjectNode();
        value.put("type", request.type().name());
        switch (request.type()) {
            case MULTIPLE_CHOICE -> {
                ArrayNode optionIds = value.putArray("optionIds");
                request.optionIds().stream()
                    .sorted(java.util.Comparator.comparing(UUID::toString))
                    .forEach(id -> optionIds.add(id.toString()));
            }
            case MATCHING -> {
                ArrayNode pairs = value.putArray("pairs");
                request.pairs().stream()
                    .sorted(java.util.Comparator.comparing(pair -> pair.leftId().toString()))
                    .forEach(pair -> {
                        ObjectNode item = pairs.addObject();
                        item.put("leftId", pair.leftId().toString());
                        item.put("rightId", pair.rightId().toString());
                    });
            }
            case FREE_TEXT -> {
                if (request.text() == null) value.putNull("text");
                else value.put("text", request.text());
            }
        }
        return value;
    }

    private void validateAnswerShape(AnswerCommand request) {
        if (request == null || request.type() == null) {
            throw ProblemException.badRequest(
                "answer.invalid_type",
                "An answer type is required."
            );
        }
        boolean hasOptions = !request.optionIds().isEmpty();
        boolean hasPairs = !request.pairs().isEmpty();
        boolean hasText = request.text() != null;
        boolean valid = switch (request.type()) {
            case MULTIPLE_CHOICE -> !hasPairs && !hasText;
            case MATCHING -> !hasOptions && !hasText;
            case FREE_TEXT -> !hasOptions && !hasPairs;
        };
        if (!valid) {
            throw ProblemException.badRequest(
                "answer.invalid_shape",
                "The answer contains fields that do not belong to its question type."
            );
        }
    }

    private AttemptResponse toResponse(
        Attempt attempt,
        AttemptQuestion current,
        QuizSession session,
        Instant now
    ) {
        int total = questions.findByAttemptIdOrderByPosition(attempt.getId()).size();
        boolean terminal = attempt.getStatus() != AttemptStatus.IN_PROGRESS;
        boolean revealScore = terminal && !session.isActiveAt(now);
        return new AttemptResponse(
            attempt.getId(), attempt.getSessionId(), attempt.getAttemptNumber(), attempt.getStatus(),
            Math.min(attempt.getCurrentPosition(), total), total,
            revealScore ? attempt.getScore() : null, attempt.getMaxScore(), attempt.getPendingReviewCount(),
            attempt.getStartedAt(), attempt.getSubmittedAt(),
            current == null ? null : studentQuestion(current),
            !terminal && current == null
        );
    }

    private StudentQuestionResponse studentQuestion(AttemptQuestion row) {
        JsonNode node = row.getQuestionSnapshot();
        QuestionType type = QuestionType.valueOf(node.path("type").asText());
        List<QuestionOptionResponse> options = new ArrayList<>();
        node.path("options").forEach(value -> options.add(new QuestionOptionResponse(
            UUID.fromString(value.path("optionId").asText()), value.path("text").asText()
        )));
        List<MatchingItemResponse> left = new ArrayList<>();
        List<Map.Entry<Integer, MatchingItemResponse>> orderedRight = new ArrayList<>();
        node.path("pairs").forEach(value -> {
            left.add(new MatchingItemResponse(
                UUID.fromString(value.path("pairId").asText()), value.path("left").asText()
            ));
            orderedRight.add(Map.entry(
                value.path("rightOrder").asInt(),
                new MatchingItemResponse(
                    UUID.fromString(value.path("rightItemId").asText()), value.path("right").asText()
                )
            ));
        });
        orderedRight.sort(Map.Entry.comparingByKey());
        return new StudentQuestionResponse(
            row.getId(), row.getPosition(), type, node.path("prompt").asText(),
            node.path("points").asInt(1), nullableText(node, "codeSnippet"),
            nullableUuid(node, "mediaId"), row.getDeadlineAt(), List.copyOf(options),
            List.copyOf(left), orderedRight.stream().map(Map.Entry::getValue).toList()
        );
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private UUID nullableUuid(JsonNode node, String field) {
        String value = nullableText(node, field);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private void publishResultChange(Attempt attempt, QuizSession session) {
        changes.resultsChanged(
            attempt.getStudentId(),
            session.getTeacherId()
        );
    }

    private record LockedAttempt(Attempt attempt, QuizSession session) {}

    private record Grade(Integer points, GradingState state) {}
}
