package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.feature.media.core.MediaAccessContributor;
import com.sosehl.curtis.feature.sessions.attempt.Attempt;
import com.sosehl.curtis.feature.sessions.attempt.AttemptQuestion;
import com.sosehl.curtis.feature.sessions.attempt.AttemptQuestionRepository;
import com.sosehl.curtis.feature.sessions.attempt.AttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
@SOSE_ReadOnlyTransaction
public class SessionMediaAccess implements MediaAccessContributor {
    private final QuizSessionRepository sessions;
    private final AttemptRepository attempts;
    private final AttemptQuestionRepository questions;
    private final Clock clock;

    public SessionMediaAccess(
        QuizSessionRepository sessions,
        AttemptRepository attempts,
        AttemptQuestionRepository questions,
        Clock clock
    ) {
        this.sessions = sessions;
        this.attempts = attempts;
        this.questions = questions;
        this.clock = clock;
    }

    @Override
    public boolean canRead(UUID userId, UUID mediaId) {
        if (teacherSessionContains(userId, mediaId)) {
            return true;
        }

        List<Attempt> studentAttempts = attempts.findByStudentId(userId);
        if (studentAttempts.isEmpty()) {
            return false;
        }
        Map<UUID, Attempt> attemptsById = studentAttempts
            .stream()
            .collect(Collectors.toMap(Attempt::getId, Function.identity()));
        Map<UUID, QuizSession> sessionsById = sessions
            .findAllById(sessionIds(studentAttempts))
            .stream()
            .collect(Collectors.toMap(QuizSession::getId, Function.identity()));
        Instant now = clock.instant();
        return questions
            .findByAttemptIdIn(attemptsById.keySet())
            .stream()
            .filter(question -> references(question, mediaId))
            .anyMatch(question -> isVisible(
                question,
                attemptsById,
                sessionsById,
                now
            ));
    }

    private boolean teacherSessionContains(UUID teacherId, UUID mediaId) {
        String expected = mediaId.toString();
        for (QuizSession session : sessions.findByTeacherIdOrderByStartedAtDesc(teacherId)) {
            for (var question : session.getQuizSnapshot().path("questions")) {
                if (expected.equals(question.path("mediaId").asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Collection<UUID> sessionIds(List<Attempt> values) {
        return values
            .stream()
            .map(Attempt::getSessionId)
            .collect(Collectors.toSet());
    }

    private boolean references(AttemptQuestion question, UUID mediaId) {
        return mediaId.toString().equals(
            question.getQuestionSnapshot().path("mediaId").asText()
        );
    }

    private boolean isVisible(
        AttemptQuestion question,
        Map<UUID, Attempt> attemptsById,
        Map<UUID, QuizSession> sessionsById,
        Instant now
    ) {
        Attempt attempt = attemptsById.get(question.getAttemptId());
        if (attempt == null) {
            return false;
        }
        QuizSession session = sessionsById.get(attempt.getSessionId());
        return session != null && (
            question.getServedAt() != null || !session.isActiveAt(now)
        );
    }
}
