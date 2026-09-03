package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.sessions.core.AttemptStatus;
import java.util.Collection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AttemptRepository extends JpaRepository<Attempt, UUID> {
    Optional<Attempt> findByIdAndStudentId(UUID id, UUID studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Attempt> findLockedById(UUID id);

    Optional<Attempt> findFirstBySessionIdAndStudentIdAndStatusOrderByAttemptNumberDesc(
        UUID sessionId,
        UUID studentId,
        AttemptStatus status
    );

    int countBySessionIdAndStudentId(UUID sessionId, UUID studentId);

    List<Attempt> findBySessionIdOrderByStartedAtDesc(UUID sessionId);

    List<Attempt> findByStudentIdAndStatusNotOrderByStartedAtDesc(
        UUID studentId,
        AttemptStatus excludedStatus,
        Pageable pageable
    );

    List<Attempt> findByStudentIdAndStatusNotAndSessionIdNotInOrderByStartedAtDesc(
        UUID studentId,
        AttemptStatus excludedStatus,
        Collection<UUID> hiddenSessionIds,
        Pageable pageable
    );

    List<Attempt> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndSessionTeacherId(
        UUID studentId,
        UUID teacherId
    );

    List<Attempt> findByStudentIdAndSessionTeacherIdAndPendingReviewCountAndStatusIn(
        UUID studentId,
        UUID teacherId,
        int pendingReviewCount,
        Collection<AttemptStatus> statuses
    );

    List<Attempt> findBySessionIdInAndPendingReviewCountAndStatusIn(
        Collection<UUID> sessionIds,
        int pendingReviewCount,
        Collection<AttemptStatus> statuses
    );

    List<Attempt> findBySessionIdAndStatus(UUID sessionId, AttemptStatus status);

    List<Attempt> findBySessionIdIn(List<UUID> sessionIds);
}
