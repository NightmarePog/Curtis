package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sosehl.curtis.feature.quizzes.core.QuestionType;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.core.AttemptQuestionState;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.GradingState;
import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.feature.sessions.core.SessionStatus;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptDetailResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.MatchingPairResultResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.QuestionOptionResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.QuestionResultResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.ReviewResponse;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class AttemptQueryService {
    private static final int MAX_HISTORY = 200;
    private final QuizSessionRepository sessions;
    private final AttemptRepository attempts;
    private final AttemptQuestionRepository questions;
    private final UserDirectory users;
    private final ObjectMapper objectMapper;
    private final SessionChangeNotifier changes;
    private final Clock clock;

    public AttemptQueryService(
        QuizSessionRepository sessions,
        AttemptRepository attempts,
        AttemptQuestionRepository questions,
        UserDirectory users,
        ObjectMapper objectMapper,
        SessionChangeNotifier changes,
        Clock clock
    ) {
        this.sessions = sessions;
        this.attempts = attempts;
        this.questions = questions;
        this.users = users;
        this.objectMapper = objectMapper;
        this.changes = changes;
        this.clock = clock;
    }

    public List<AttemptSummaryResponse> listForTeacherSession(UUID sessionId, UUID teacherId) {
        QuizSession session = requireSession(sessionId);
        requireOwner(session, teacherId);
        return summaries(attempts.findBySessionIdOrderByStartedAtDesc(sessionId), Map.of(sessionId, session));
    }

    public List<AttemptSummaryResponse> historyForTeacher(UUID teacherId, int limit) {
        List<QuizSession> owned = sessions.findByTeacherIdOrderByStartedAtDesc(teacherId);
        if (owned.isEmpty()) return List.of();
        Map<UUID, QuizSession> sessionMap = owned.stream()
            .collect(Collectors.toMap(QuizSession::getId, value -> value));
        List<Attempt> values = attempts.findBySessionIdIn(new ArrayList<>(sessionMap.keySet()));
        values.sort(java.util.Comparator.comparing(
            Attempt::getStartedAt,
            java.util.Comparator.reverseOrder()
        ));
        int bounded = Math.max(1, Math.min(limit, MAX_HISTORY));
        return summaries(values.stream().limit(bounded).toList(), sessionMap);
    }

    public List<AttemptSummaryResponse> listForAdminSession(UUID sessionId) {
        QuizSession session = requireSession(sessionId);
        return summaries(attempts.findBySessionIdOrderByStartedAtDesc(sessionId), Map.of(sessionId, session));
    }

    public List<AttemptSummaryResponse> listForStudent(UUID studentId, int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_HISTORY));
        Instant now = clock.instant();
        List<UUID> hiddenSessionIds = sessions
            .findByStatusAndClosesAtAfter(SessionStatus.ACTIVE, now)
            .stream()
            .map(QuizSession::getId)
            .toList();
        PageRequest firstPage = PageRequest.of(0, bounded);
        List<Attempt> values = hiddenSessionIds.isEmpty()
            ? attempts.findByStudentIdAndStatusNotOrderByStartedAtDesc(
                studentId,
                AttemptStatus.IN_PROGRESS,
                firstPage
            )
            : attempts.findByStudentIdAndStatusNotAndSessionIdNotInOrderByStartedAtDesc(
                studentId,
                AttemptStatus.IN_PROGRESS,
                hiddenSessionIds,
                firstPage
            );
        return summaries(values, sessionMap(values));
    }

    public AttemptDetailResponse detailForTeacher(UUID attemptId, UUID teacherId) {
        return detail(attemptId, attempt -> {
            QuizSession session = requireSession(attempt.getSessionId());
            return session.getTeacherId().equals(teacherId);
        }, true);
    }

    public AttemptDetailResponse detailForAdmin(UUID attemptId) {
        return detail(attemptId, ignored -> true, true);
    }

    public AttemptDetailResponse detailForStudent(UUID attemptId, UUID studentId) {
        Attempt attempt = requireAttempt(attemptId);
        if (!attempt.getStudentId().equals(studentId)) {
            throw ProblemException.notFound("attempt.not_found", "The attempt was not found.");
        }
        QuizSession session = requireSession(attempt.getSessionId());
        if (session.isActiveAt(clock.instant())) {
            throw ProblemException.conflict(
                "result.not_available",
                "Results are available after the session closes."
            );
        }
        return detail(attempt, true);
    }

    public List<ReviewResponse> pendingForTeacher(UUID teacherId) {
        return pending(session -> session.getTeacherId().equals(teacherId));
    }

    public List<ReviewResponse> pendingForAdmin() {
        return pending(ignored -> true);
    }

    @Transactional
    public QuestionResultResponse gradeForTeacher(
        UUID questionResultId,
        int points,
        UUID teacherId
    ) {
        return grade(questionResultId, points, teacherId, false);
    }

    @Transactional
    public QuestionResultResponse gradeForAdmin(
        UUID questionResultId,
        int points,
        UUID administratorId
    ) {
        return grade(questionResultId, points, administratorId, true);
    }

    private QuestionResultResponse grade(
        UUID id,
        int awarded,
        UUID grader,
        boolean administrator
    ) {
        AttemptQuestion reference = questions.findById(id).orElseThrow(() ->
            ProblemException.notFound("review.not_found", "The review item was not found.")
        );
        Attempt attempt = attempts.findLockedById(reference.getAttemptId()).orElseThrow(() ->
            ProblemException.notFound("attempt.not_found", "The attempt was not found.")
        );
        AttemptQuestion question = questions.findLockedById(id).orElseThrow(() ->
            ProblemException.notFound("review.not_found", "The review item was not found.")
        );
        if (!question.getAttemptId().equals(attempt.getId())) {
            throw ProblemException.notFound(
                "review.not_found",
                "The review item was not found."
            );
        }
        QuizSession session = requireSession(attempt.getSessionId());
        if (!administrator && !session.getTeacherId().equals(grader)) {
            throw ProblemException.notFound("review.not_found", "The review item was not found.");
        }
        if (!QuestionType.FREE_TEXT.name().equals(question.getQuestionSnapshot().path("type").asText())) {
            throw ProblemException.badRequest("review.not_free_text", "Only free-text answers can be graded manually.");
        }
        if (
            question.getState() != AttemptQuestionState.ANSWERED ||
            question.getGradingState() != GradingState.PENDING_REVIEW
        ) {
            throw ProblemException.conflict(
                "review.not_pending",
                "Only an answered item that is pending review can be graded."
            );
        }
        if (attempt.getStatus() != AttemptStatus.PENDING_REVIEW) {
            throw ProblemException.conflict(
                "review.attempt_in_progress",
                "The attempt must be submitted before an answer can be graded."
            );
        }
        if (awarded < 0 || awarded > question.getMaxPoints()) {
            throw ProblemException.badRequest("review.points_out_of_range", "The awarded points are outside the allowed range.");
        }
        question.manuallyGrade(awarded, grader, clock.instant());
        recalculate(attempt);
        changes.resultsChanged(
            attempt.getStudentId(),
            session.getTeacherId()
        );
        return questionResponse(question, true);
    }

    private void recalculate(Attempt attempt) {
        List<AttemptQuestion> values = questions.findByAttemptIdOrderByPosition(attempt.getId());
        int pending = (int) values.stream()
            .filter(value -> value.getGradingState() == GradingState.PENDING_REVIEW)
            .count();
        int score = values.stream()
            .map(AttemptQuestion::getAwardedPoints)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
        attempt.applyGradeTotals(score, pending, clock.instant());
    }

    private AttemptDetailResponse detail(
        UUID attemptId,
        Predicate<Attempt> access,
        boolean revealCorrect
    ) {
        Attempt attempt = requireAttempt(attemptId);
        if (!access.test(attempt)) {
            throw ProblemException.notFound("attempt.not_found", "The attempt was not found.");
        }
        return detail(attempt, revealCorrect);
    }

    private AttemptDetailResponse detail(Attempt attempt, boolean revealCorrect) {
        QuizSession session = requireSession(attempt.getSessionId());
        AttemptSummaryResponse summary = summaries(List.of(attempt), Map.of(session.getId(), session)).get(0);
        List<QuestionResultResponse> results = questions
            .findByAttemptIdOrderByPosition(attempt.getId())
            .stream()
            .map(question -> questionResponse(question, revealCorrect))
            .toList();
        return new AttemptDetailResponse(summary, results);
    }

    private List<ReviewResponse> pending(Predicate<QuizSession> access) {
        List<AttemptQuestion> pending = questions.findByGradingStateOrderByAnsweredAtAsc(
            GradingState.PENDING_REVIEW
        );
        Map<UUID, Attempt> attemptMap = attempts.findAllById(
            pending.stream().map(AttemptQuestion::getAttemptId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Attempt::getId, value -> value));
        Map<UUID, QuizSession> sessionMap = sessions.findAllById(
            attemptMap.values().stream().map(Attempt::getSessionId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(QuizSession::getId, value -> value));
        Map<UUID, UserSummary> userMap = userMap(
            attemptMap.values().stream().map(Attempt::getStudentId).toList()
        );
        List<ReviewResponse> result = new ArrayList<>();
        for (AttemptQuestion question : pending) {
            Attempt attempt = attemptMap.get(question.getAttemptId());
            if (
                attempt == null ||
                attempt.getStatus() != AttemptStatus.PENDING_REVIEW
            ) continue;
            QuizSession session = sessionMap.get(attempt.getSessionId());
            if (session == null || !access.test(session)) continue;
            UserSummary student = userMap.get(attempt.getStudentId());
            result.add(new ReviewResponse(
                question.getId(), attempt.getId(), session.getId(), session.getTitle(),
                attempt.getStudentId(), displayName(student, attempt.getStudentId()),
                question.getQuestionSnapshot().path("prompt").asText(),
                question.getResponse() == null ? "" : question.getResponse().path("text").asText(""),
                question.getMaxPoints(), question.getAwardedPoints(), question.getAnsweredAt()
            ));
        }
        return List.copyOf(result);
    }

    private List<AttemptSummaryResponse> summaries(
        List<Attempt> values,
        Map<UUID, QuizSession> sessionMap
    ) {
        Map<UUID, UserSummary> userMap = userMap(
            values.stream().map(Attempt::getStudentId).toList()
        );
        return values.stream().map(attempt -> {
            QuizSession session = sessionMap.get(attempt.getSessionId());
            UserSummary student = userMap.get(attempt.getStudentId());
            return new AttemptSummaryResponse(
                attempt.getId(), attempt.getSessionId(),
                session == null ? "Deleted session" : session.getTitle(),
                attempt.getStudentId(), displayName(student, attempt.getStudentId()),
                attempt.getAttemptNumber(), attempt.getStatus(), attempt.getScore(),
                attempt.getMaxScore(), percentage(attempt.getScore(), attempt.getMaxScore()),
                attempt.getPendingReviewCount(), attempt.getStartedAt(), attempt.getSubmittedAt()
            );
        }).toList();
    }

    private QuestionResultResponse questionResponse(AttemptQuestion question, boolean revealCorrect) {
        JsonNode snapshot = question.getQuestionSnapshot();
        return new QuestionResultResponse(
            question.getId(), question.getPosition(), snapshot.path("prompt").asText(),
            QuestionType.valueOf(snapshot.path("type").asText()), question.getMaxPoints(),
            question.getAwardedPoints(),
            question.getState(), question.getGradingState(), options(snapshot),
            matchingPairs(snapshot), question.getResponse(),
            revealCorrect ? correctAnswer(snapshot) : null, question.getAnsweredAt()
        );
    }

    private List<QuestionOptionResponse> options(JsonNode snapshot) {
        List<QuestionOptionResponse> result = new ArrayList<>();
        snapshot.path("options").forEach(option -> result.add(
            new QuestionOptionResponse(
                UUID.fromString(option.path("optionId").asText()),
                option.path("text").asText()
            )
        ));
        return List.copyOf(result);
    }

    private List<MatchingPairResultResponse> matchingPairs(JsonNode snapshot) {
        List<MatchingPairResultResponse> result = new ArrayList<>();
        snapshot.path("pairs").forEach(pair -> result.add(
            new MatchingPairResultResponse(
                UUID.fromString(pair.path("pairId").asText()),
                pair.path("left").asText(),
                UUID.fromString(pair.path("rightItemId").asText()),
                pair.path("right").asText()
            )
        ));
        return List.copyOf(result);
    }

    private JsonNode correctAnswer(JsonNode snapshot) {
        ObjectNode result = objectMapper.createObjectNode();
        String type = snapshot.path("type").asText();
        result.put("type", type);
        if (QuestionType.MULTIPLE_CHOICE.name().equals(type)) {
            ArrayNode ids = result.putArray("optionIds");
            snapshot.path("options").forEach(option -> {
                if (option.path("correct").asBoolean()) ids.add(option.path("optionId").asText());
            });
        } else if (QuestionType.MATCHING.name().equals(type)) {
            ArrayNode pairs = result.putArray("pairs");
            snapshot.path("pairs").forEach(pair -> {
                ObjectNode value = pairs.addObject();
                value.put("leftId", pair.path("pairId").asText());
                value.put("rightId", pair.path("rightItemId").asText());
            });
        }
        return result;
    }

    private Map<UUID, QuizSession> sessionMap(List<Attempt> values) {
        Set<UUID> ids = values.stream().map(Attempt::getSessionId).collect(Collectors.toSet());
        return sessions.findAllById(ids).stream()
            .collect(Collectors.toMap(QuizSession::getId, value -> value));
    }

    private Map<UUID, UserSummary> userMap(Collection<UUID> ids) {
        Map<UUID, UserSummary> values = new HashMap<>();
        users.summariesByIds(ids).forEach(user -> values.put(user.id(), user));
        return values;
    }

    private String displayName(UserSummary user, UUID fallback) {
        return user == null ? fallback.toString() : user.displayName();
    }

    private int percentage(int score, int maxScore) {
        return maxScore <= 0 ? 0 : (int) Math.round(score * 100.0 / maxScore);
    }

    private Attempt requireAttempt(UUID id) {
        return attempts.findById(id).orElseThrow(() ->
            ProblemException.notFound("attempt.not_found", "The attempt was not found.")
        );
    }

    private QuizSession requireSession(UUID id) {
        return sessions.findById(id).orElseThrow(() ->
            ProblemException.notFound("session.not_found", "The session was not found.")
        );
    }

    private void requireOwner(QuizSession session, UUID teacherId) {
        if (!session.getTeacherId().equals(teacherId)) {
            throw ProblemException.notFound("session.not_found", "The session was not found.");
        }
    }
}
