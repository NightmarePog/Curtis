package com.sosehl.curtis.feature.sessions.ranking;

import com.sosehl.curtis.feature.classrooms.core.ClassroomDirectory;
import com.sosehl.curtis.feature.classrooms.core.ClassroomRoster;
import com.sosehl.curtis.feature.classrooms.core.GroupRoster;
import com.sosehl.curtis.feature.sessions.QuizSession;
import com.sosehl.curtis.feature.sessions.QuizSessionRepository;
import com.sosehl.curtis.feature.sessions.SessionParticipant;
import com.sosehl.curtis.feature.sessions.SessionParticipantGroup;
import com.sosehl.curtis.feature.sessions.SessionParticipantGroupRepository;
import com.sosehl.curtis.feature.sessions.SessionParticipantRepository;
import com.sosehl.curtis.feature.sessions.attempt.Attempt;
import com.sosehl.curtis.feature.sessions.attempt.AttemptRepository;
import com.sosehl.curtis.feature.sessions.attempt.AttemptScoreCalculator;
import com.sosehl.curtis.feature.sessions.attempt.AttemptScoreCalculator.ScoreTotal;
import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import com.sosehl.curtis.feature.sessions.core.ScorePolicy;
import com.sosehl.curtis.feature.sessions.ranking.dto.RankingMemberResponse;
import com.sosehl.curtis.feature.sessions.ranking.dto.RankingResponse;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
@SOSE_ReadOnlyTransaction
public class RankingService {

    private static final Set<AttemptStatus> SCORED_STATUSES = Set.of(
        AttemptStatus.GRADED,
        AttemptStatus.EXPIRED
    );

    private final ClassroomDirectory classrooms;
    private final UserDirectory users;
    private final QuizSessionRepository sessions;
    private final SessionParticipantRepository participants;
    private final SessionParticipantGroupRepository participantGroups;
    private final AttemptRepository attempts;
    private final Clock clock;

    public RankingService(
        ClassroomDirectory classrooms,
        UserDirectory users,
        QuizSessionRepository sessions,
        SessionParticipantRepository participants,
        SessionParticipantGroupRepository participantGroups,
        AttemptRepository attempts,
        Clock clock
    ) {
        this.classrooms = classrooms;
        this.users = users;
        this.sessions = sessions;
        this.participants = participants;
        this.participantGroups = participantGroups;
        this.attempts = attempts;
        this.clock = clock;
    }

    public List<RankingResponse> forStudent(UUID studentId) {
        ClassroomRoster classroom = classrooms
            .currentForStudent(studentId)
            .filter(ClassroomRoster::active)
            .orElse(null);
        if (classroom == null) return List.of();

        List<RankingResponse> result = new ArrayList<>();
        result.add(buildClass(classroom, studentId));
        classroom
            .activeGroups()
            .stream()
            .filter(group -> group.activeStudentIds().contains(studentId))
            .map(group -> buildGroup(group, studentId))
            .forEach(result::add);
        return List.copyOf(result);
    }

    private RankingResponse buildClass(
        ClassroomRoster classroom,
        UUID currentStudent
    ) {
        List<QuizSession> targetedSessions = visibleSessions(
            sessions.findDistinctByClassTargets_ClassId(classroom.id())
        );
        Set<ParticipantKey> eligibleParticipants = classParticipants(
            classroom.id(),
            targetedSessions
        );
        return new RankingResponse(
            "CLASS",
            classroom.id(),
            classroom.id(),
            classroom.name(),
            ranked(
                classroom.activeStudentIds(),
                totals(targetedSessions, eligibleParticipants),
                currentStudent
            )
        );
    }

    private RankingResponse buildGroup(
        GroupRoster group,
        UUID currentStudent
    ) {
        List<QuizSession> targetedSessions = visibleSessions(
            sessions.findDistinctByGroupTargets_GroupId(group.id())
        );
        Set<ParticipantKey> eligibleParticipants = groupParticipants(
            group.id(),
            targetedSessions
        );
        return new RankingResponse(
            "GROUP",
            group.id(),
            group.classId(),
            group.name(),
            ranked(
                group.activeStudentIds(),
                totals(targetedSessions, eligibleParticipants),
                currentStudent
            )
        );
    }

    private List<QuizSession> visibleSessions(
        List<QuizSession> targetedSessions
    ) {
        return targetedSessions
            .stream()
            .filter(session -> !session.isActiveAt(clock.instant()))
            .toList();
    }

    private Set<ParticipantKey> classParticipants(
        UUID classId,
        List<QuizSession> targetedSessions
    ) {
        List<UUID> sessionIds = sessionIds(targetedSessions);
        if (sessionIds.isEmpty()) return Set.of();
        return participants
            .findByClassIdAndId_SessionIdIn(classId, sessionIds)
            .stream()
            .map(this::key)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<ParticipantKey> groupParticipants(
        UUID groupId,
        List<QuizSession> targetedSessions
    ) {
        List<UUID> sessionIds = sessionIds(targetedSessions);
        if (sessionIds.isEmpty()) return Set.of();
        return participantGroups
            .findById_GroupIdAndId_SessionIdIn(groupId, sessionIds)
            .stream()
            .map(this::key)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Map<UUID, ScoreTotal> totals(
        List<QuizSession> targetedSessions,
        Set<ParticipantKey> eligibleParticipants
    ) {
        if (targetedSessions.isEmpty() || eligibleParticipants.isEmpty()) {
            return Map.of();
        }
        List<Attempt> eligibleAttempts = attempts
            .findBySessionIdInAndPendingReviewCountAndStatusIn(
                sessionIds(targetedSessions),
                0,
                SCORED_STATUSES
            )
            .stream()
            .filter(attempt -> eligibleParticipants.contains(key(attempt)))
            .toList();
        return AttemptScoreCalculator.byStudent(
            eligibleAttempts,
            scorePolicies(targetedSessions)
        );
    }

    private List<RankingMemberResponse> ranked(
        Collection<UUID> studentIds,
        Map<UUID, ScoreTotal> totals,
        UUID currentStudent
    ) {
        Map<UUID, UserSummary> students = users
            .summariesByIds(studentIds)
            .stream()
            .collect(Collectors.toMap(UserSummary::id, Function.identity()));
        List<RankSeed> values = studentIds
            .stream()
            .distinct()
            .map(studentId -> {
                ScoreTotal total = totals.getOrDefault(
                    studentId,
                    ScoreTotal.EMPTY
                );
                UserSummary student = students.get(studentId);
                return new RankSeed(
                    studentId,
                    student == null
                        ? studentId.toString()
                        : student.displayName(),
                    total
                );
            })
            .sorted(
                Comparator
                    .comparingInt(RankSeed::percentage)
                    .reversed()
                    .thenComparing(
                        Comparator.comparingLong(
                            (RankSeed value) -> value.total().score()
                        ).reversed()
                    )
                    .thenComparing(
                        RankSeed::name,
                        String.CASE_INSENSITIVE_ORDER
                    )
            )
            .toList();

        List<RankingMemberResponse> result = new ArrayList<>();
        int rank = 0;
        RankSeed previous = null;
        for (int index = 0; index < values.size(); index++) {
            RankSeed value = values.get(index);
            if (
                previous == null ||
                previous.percentage() != value.percentage() ||
                previous.total().score() != value.total().score()
            ) {
                rank = index + 1;
            }
            result.add(
                new RankingMemberResponse(
                    value.name(),
                    value.total().score(),
                    value.total().maxScore(),
                    value.percentage(),
                    value.total().attempts(),
                    rank,
                    value.id().equals(currentStudent)
                )
            );
            previous = value;
        }
        return List.copyOf(result);
    }

    private List<UUID> sessionIds(List<QuizSession> values) {
        return values.stream().map(QuizSession::getId).toList();
    }

    private Map<UUID, ScorePolicy> scorePolicies(
        List<QuizSession> values
    ) {
        return values
            .stream()
            .collect(
                Collectors.toMap(
                    QuizSession::getId,
                    QuizSession::getScorePolicy
                )
            );
    }

    private ParticipantKey key(SessionParticipant participant) {
        return new ParticipantKey(
            participant.getId().getSessionId(),
            participant.getId().getStudentId()
        );
    }

    private ParticipantKey key(SessionParticipantGroup participant) {
        return new ParticipantKey(
            participant.getId().getSessionId(),
            participant.getId().getStudentId()
        );
    }

    private ParticipantKey key(Attempt attempt) {
        return new ParticipantKey(
            attempt.getSessionId(),
            attempt.getStudentId()
        );
    }

    private record ParticipantKey(UUID sessionId, UUID studentId) {}

    private record RankSeed(UUID id, String name, ScoreTotal total) {
        private int percentage() {
            return total.percentage();
        }
    }
}
