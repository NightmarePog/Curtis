package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.sessions.core.GradingState;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, UUID> {
    List<AttemptQuestion> findByAttemptIdOrderByPosition(UUID attemptId);

    Optional<AttemptQuestion> findByAttemptIdAndPosition(UUID attemptId, int position);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AttemptQuestion> findLockedById(UUID id);

    List<AttemptQuestion> findByAttemptIdIn(Collection<UUID> attemptIds);

    List<AttemptQuestion> findByGradingStateOrderByAnsweredAtAsc(GradingState gradingState);
}
