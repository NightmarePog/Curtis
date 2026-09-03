package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.core.SessionStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface QuizSessionRepository extends JpaRepository<QuizSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QuizSession> findLockedById(UUID id);

    List<QuizSession> findByTeacherIdOrderByStartedAtDesc(UUID teacherId);

    List<QuizSession> findByStatusAndClosesAtLessThanEqual(SessionStatus status, Instant now);

    List<QuizSession> findByStatusAndClosesAtAfter(
        SessionStatus status,
        Instant now
    );

    List<QuizSession> findByIdInAndStatusAndClosesAtAfterOrderByStartedAtDesc(
        Collection<UUID> ids,
        SessionStatus status,
        Instant now
    );

    List<QuizSession> findDistinctByClassTargets_ClassId(UUID classId);

    List<QuizSession> findDistinctByGroupTargets_GroupId(UUID groupId);
}
