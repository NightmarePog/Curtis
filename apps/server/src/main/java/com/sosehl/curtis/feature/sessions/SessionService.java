package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis.feature.classrooms.core.ClassroomAudienceResolver;
import com.sosehl.curtis.feature.classrooms.core.ResolvedAudience;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshot;
import com.sosehl.curtis.feature.quizzes.core.QuizSnapshotProvider;
import com.sosehl.curtis.feature.sessions.attempt.AttemptService;
import com.sosehl.curtis.feature.sessions.core.SessionChangeNotifier;
import com.sosehl.curtis.feature.sessions.core.SessionStatus;
import com.sosehl.curtis.feature.sessions.dto.SessionResponse;
import com.sosehl.curtis.feature.sessions.dto.TargetResponse;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class SessionService {
    private final QuizSessionRepository sessions;
    private final SessionParticipantRepository participants;
    private final SessionParticipantGroupRepository participantGroups;
    private final ClassroomAudienceResolver classrooms;
    private final QuizSnapshotProvider quizzes;
    private final ObjectMapper objectMapper;
    private final SessionChangeNotifier changes;
    private final Clock clock;
    private final AttemptService attempts;

    public SessionService(
        QuizSessionRepository sessions,
        SessionParticipantRepository participants,
        SessionParticipantGroupRepository participantGroups,
        ClassroomAudienceResolver classrooms,
        QuizSnapshotProvider quizzes,
        ObjectMapper objectMapper,
        SessionChangeNotifier changes,
        AttemptService attempts,
        Clock clock
    ) {
        this.sessions = sessions;
        this.participants = participants;
        this.participantGroups = participantGroups;
        this.classrooms = classrooms;
        this.quizzes = quizzes;
        this.objectMapper = objectMapper;
        this.changes = changes;
        this.attempts = attempts;
        this.clock = clock;
    }

    @Transactional
    public SessionResponse create(
        CreateSessionCommand request,
        UUID teacherId,
        String teacherName
    ) {
        Instant now = clock.instant();
        if (!request.closesAt().isAfter(now)) {
            throw ProblemException.badRequest("session.invalid_deadline", "The session deadline must be in the future.");
        }

        QuizSnapshot snapshot = quizzes.loadLaunchable(request.quizId(), teacherId);
        ResolvedAudience audience = classrooms.resolveAudience(
            teacherId, request.classIds(), request.groupIds()
        );
        UUID sessionId = UUID.randomUUID();
        QuizSession session = new QuizSession(
            sessionId,
            snapshot.quizId(),
            teacherId,
            snapshot.title(),
            snapshot.description(),
            snapshot.subjectName(),
            snapshot.chapter(),
            teacherName,
            objectMapper.valueToTree(snapshot),
            now,
            request.closesAt(),
            request.maxAttempts(),
            request.scorePolicy()
        );
        audience.classes().forEach(value -> session.getClassTargets().add(
            new SessionClassTarget(value.id(), value.name())
        ));
        audience.groups().forEach(value -> session.getGroupTargets().add(
            new SessionGroupTarget(value.id(), value.classId(), value.name())
        ));
        sessions.saveAndFlush(session);

        List<SessionParticipant> participantRows = audience.students().stream()
            .map(value -> new SessionParticipant(sessionId, value.studentId(), value.classId()))
            .toList();
        participants.saveAll(participantRows);
        List<SessionParticipantGroup> groupRows = audience.students().stream()
            .flatMap(student -> student.targetedGroupIds().stream().map(groupId ->
                new SessionParticipantGroup(
                    sessionId,
                    student.studentId(),
                    groupId,
                    student.classId()
                )
            ))
            .toList();
        participantGroups.saveAll(groupRows);

        Set<UUID> recipients = new HashSet<>();
        recipients.add(teacherId);
        audience.students().forEach(student -> recipients.add(student.studentId()));
        changes.sessionsChanged(recipients);
        return toResponse(session);
    }

    public List<SessionResponse> listForTeacher(UUID teacherId) {
        return sessions.findByTeacherIdOrderByStartedAtDesc(teacherId).stream()
            .map(this::toResponse)
            .toList();
    }

    public List<SessionResponse> listForAdministrator() {
        return sessions.findAll(Sort.by(Sort.Order.desc("startedAt"))).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public List<SessionResponse> listForStudent(UUID studentId) {
        expireDueSessions();
        List<UUID> sessionIds = participants
            .findByIdStudentId(studentId)
            .stream()
            .map(participant -> participant.getId().getSessionId())
            .toList();
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        return sessions
            .findByIdInAndStatusAndClosesAtAfterOrderByStartedAtDesc(
                sessionIds,
                SessionStatus.ACTIVE,
                clock.instant()
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SessionResponse closeOwned(UUID sessionId, UUID teacherId) {
        QuizSession session = locked(sessionId);
        if (!session.getTeacherId().equals(teacherId)) {
            throw ProblemException.notFound("session.not_found", "The session was not found.");
        }
        closeInternal(session, SessionStatus.CLOSED);
        return toResponse(session);
    }

    @Transactional
    public SessionResponse closeAsAdministrator(UUID sessionId) {
        QuizSession session = locked(sessionId);
        closeInternal(session, SessionStatus.CLOSED);
        return toResponse(session);
    }

    @Transactional
    public void expireDueSessions() {
        Instant now = clock.instant();
        sessions.findByStatusAndClosesAtLessThanEqual(SessionStatus.ACTIVE, now)
            .forEach(session -> closeInternal(session, SessionStatus.EXPIRED));
    }

    private QuizSession locked(UUID id) {
        return sessions.findLockedById(id).orElseThrow(() ->
            ProblemException.notFound("session.not_found", "The session was not found.")
        );
    }

    private void closeInternal(QuizSession session, SessionStatus terminal) {
        if (session.getStatus() != SessionStatus.ACTIVE) return;
        Instant now = clock.instant();
        session.close(terminal, now);
        attempts.hardStopSession(session.getId(), now);
        Set<UUID> recipients = new HashSet<>();
        recipients.add(session.getTeacherId());
        participants.findByIdSessionId(session.getId()).forEach(value ->
            recipients.add(value.getId().getStudentId())
        );
        changes.sessionsChanged(recipients);
    }

    SessionResponse toResponse(QuizSession session) {
        List<TargetResponse> targets = new ArrayList<>();
        session.getClassTargets().stream()
            .sorted(java.util.Comparator.comparing(SessionClassTarget::getName))
            .forEach(value -> targets.add(new TargetResponse(
                value.getClassId(), value.getClassId(), value.getName(), "CLASS"
            )));
        session.getGroupTargets().stream()
            .sorted(java.util.Comparator.comparing(SessionGroupTarget::getName))
            .forEach(value -> targets.add(new TargetResponse(
                value.getGroupId(), value.getClassId(), value.getName(), "GROUP"
            )));
        int available = session.getQuizSnapshot().path("questions").size();
        int requested = session.getQuizSnapshot().path("maxQuestionsPerSession").asInt(available);
        return new SessionResponse(
            session.getId(), session.getQuizId(), session.getTitle(), session.getDescription(),
            session.getSubject(), session.getChapter(), session.getTeacherName(), session.getStatus(),
            session.getStartedAt(), session.getClosesAt(), Math.min(available, requested),
            session.getMaxAttempts(), session.getScorePolicy(), List.copyOf(targets)
        );
    }
}
