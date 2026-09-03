package com.sosehl.curtis.feature.sessions.students;

import com.sosehl.curtis.feature.classrooms.core.ClassroomDirectory;
import com.sosehl.curtis.feature.classrooms.core.ClassroomRoster;
import com.sosehl.curtis.feature.classrooms.core.GroupRoster;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.attempt.Attempt;
import com.sosehl.curtis.feature.sessions.attempt.AttemptQueryService;
import com.sosehl.curtis.feature.sessions.attempt.AttemptRepository;
import com.sosehl.curtis.feature.sessions.attempt.AttemptScoreCalculator;
import com.sosehl.curtis.feature.sessions.attempt.AttemptScoreCalculator.ScoreTotal;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.dto.TargetResponse;
import com.sosehl.curtis.feature.sessions.students.dto.TeacherClassStudentsResponse;
import com.sosehl.curtis.feature.sessions.students.dto.TeacherStudentProfileResponse;
import com.sosehl.curtis.feature.sessions.students.dto.TeacherStudentResponse;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
@SOSE_ReadOnlyTransaction
public class TeacherStudentQueryService {

    private static final Set<AttemptStatus> SCORED_STATUSES = Set.of(
        AttemptStatus.GRADED,
        AttemptStatus.EXPIRED
    );

    private final ClassroomDirectory classrooms;
    private final UserDirectory users;
    private final QuizSessionRepository sessions;
    private final AttemptRepository attempts;
    private final AttemptQueryService attemptQueries;

    public TeacherStudentQueryService(
        ClassroomDirectory classrooms,
        UserDirectory users,
        QuizSessionRepository sessions,
        AttemptRepository attempts,
        AttemptQueryService attemptQueries
    ) {
        this.classrooms = classrooms;
        this.users = users;
        this.sessions = sessions;
        this.attempts = attempts;
        this.attemptQueries = attemptQueries;
    }

    public List<TeacherClassStudentsResponse> list(UUID teacherId) {
        List<ClassroomRoster> rosters = classrooms.activeForTeacher(teacherId);
        Set<UUID> studentIds = rosters
            .stream()
            .map(ClassroomRoster::activeStudentIds)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Map<UUID, UserSummary> students = userMap(studentIds);
        Map<UUID, ScoreTotal> totals = aggregatesForTeacher(
            teacherId,
            studentIds
        );

        return rosters
            .stream()
            .map(roster ->
                new TeacherClassStudentsResponse(
                    roster.id(),
                    roster.name(),
                    studentsForClass(roster, students, totals)
                )
            )
            .toList();
    }

    public TeacherStudentProfileResponse profile(
        UUID teacherId,
        UUID studentId
    ) {
        ClassroomRoster currentClass = classrooms
            .currentForStudent(studentId)
            .orElse(null);
        boolean currentStudent = currentClass != null && classrooms
            .isTeacherAssigned(teacherId, currentClass.id());
        boolean historicalStudent = attempts.existsByStudentIdAndSessionTeacherId(
            studentId,
            teacherId
        );
        if (!currentStudent && !historicalStudent) {
            throw ProblemException.notFound(
                "student.not_found",
                "The student was not found."
            );
        }

        UserSummary student = users.require(studentId);
        List<QuizSession> ownedSessions = sessions
            .findByTeacherIdOrderByStartedAtDesc(teacherId);
        ScoreTotal total = aggregateForStudent(
            teacherId,
            studentId,
            ownedSessions
        );
        List<AttemptSummaryResponse> history = ownHistory(
            teacherId,
            studentId,
            ownedSessions
        );
        ClassroomRoster visibleClass = currentStudent ? currentClass : null;

        return new TeacherStudentProfileResponse(
            studentId,
            student.displayName(),
            visibleClass == null ? null : visibleClass.id(),
            visibleClass == null ? null : visibleClass.name(),
            visibleClass == null
                ? List.of()
                : groups(visibleClass, studentId),
            total.attempts(),
            total.score(),
            total.maxScore(),
            total.percentage(),
            history
        );
    }

    private List<TeacherStudentResponse> studentsForClass(
        ClassroomRoster roster,
        Map<UUID, UserSummary> students,
        Map<UUID, ScoreTotal> totals
    ) {
        return roster
            .activeStudentIds()
            .stream()
            .map(studentId -> {
                UserSummary student = students.get(studentId);
                ScoreTotal total = totals.getOrDefault(
                    studentId,
                    ScoreTotal.EMPTY
                );
                return new TeacherStudentResponse(
                    studentId,
                    displayName(student, studentId),
                    groups(roster, studentId),
                    total.attempts(),
                    total.score(),
                    total.maxScore(),
                    total.percentage(),
                    total.lastActivity()
                );
            })
            .sorted(
                Comparator.comparing(
                    TeacherStudentResponse::displayName,
                    String.CASE_INSENSITIVE_ORDER
                )
            )
            .toList();
    }

    private Map<UUID, ScoreTotal> aggregatesForTeacher(
        UUID teacherId,
        Set<UUID> studentIds
    ) {
        if (studentIds.isEmpty()) return Map.of();
        List<QuizSession> ownedSessions = sessions
            .findByTeacherIdOrderByStartedAtDesc(teacherId);
        if (ownedSessions.isEmpty()) return Map.of();

        List<UUID> sessionIds = ownedSessions
            .stream()
            .map(QuizSession::getId)
            .toList();
        List<Attempt> eligible = attempts
            .findBySessionIdInAndPendingReviewCountAndStatusIn(
                sessionIds,
                0,
                SCORED_STATUSES
            )
            .stream()
            .filter(attempt -> studentIds.contains(attempt.getStudentId()))
            .toList();
        return AttemptScoreCalculator.byStudent(
            eligible,
            scorePolicies(ownedSessions)
        );
    }

    private ScoreTotal aggregateForStudent(
        UUID teacherId,
        UUID studentId,
        List<QuizSession> ownedSessions
    ) {
        List<Attempt> eligible = attempts
            .findByStudentIdAndSessionTeacherIdAndPendingReviewCountAndStatusIn(
                studentId,
                teacherId,
                0,
                SCORED_STATUSES
            );
        return AttemptScoreCalculator
            .byStudent(eligible, scorePolicies(ownedSessions))
            .getOrDefault(studentId, ScoreTotal.EMPTY);
    }

    private Map<UUID, ScorePolicy> scorePolicies(
        List<QuizSession> ownedSessions
    ) {
        return ownedSessions
            .stream()
            .collect(
                Collectors.toMap(
                    QuizSession::getId,
                    QuizSession::getScorePolicy
                )
            );
    }

    private List<TargetResponse> groups(
        ClassroomRoster roster,
        UUID studentId
    ) {
        return roster
            .activeGroups()
            .stream()
            .filter(group -> group.activeStudentIds().contains(studentId))
            .map(group -> target(roster.id(), group))
            .toList();
    }

    private TargetResponse target(UUID classId, GroupRoster group) {
        return new TargetResponse(
            group.id(),
            classId,
            group.name(),
            "GROUP"
        );
    }

    private List<AttemptSummaryResponse> ownHistory(
        UUID teacherId,
        UUID studentId,
        List<QuizSession> ownedSessions
    ) {
        List<AttemptSummaryResponse> history = new ArrayList<>();
        for (QuizSession session : ownedSessions) {
            attemptQueries
                .listForTeacherSession(session.getId(), teacherId)
                .stream()
                .filter(value -> value.studentId().equals(studentId))
                .forEach(history::add);
        }
        history.sort(
            Comparator.comparing(
                AttemptSummaryResponse::startedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
            )
        );
        return history.stream().limit(100).toList();
    }

    private Map<UUID, UserSummary> userMap(Collection<UUID> ids) {
        Map<UUID, UserSummary> result = new HashMap<>();
        users
            .summariesByIds(ids)
            .forEach(user -> result.put(user.id(), user));
        return result;
    }

    private String displayName(UserSummary user, UUID fallback) {
        return user == null ? fallback.toString() : user.displayName();
    }
}
