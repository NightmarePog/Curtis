package com.sosehl.curtis_backend.domain.v1.snapshot;

import com.sosehl.curtis_backend.domain.v1.quiz.Quiz;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    Optional<Snapshot> findTopByQuizOrderByCreatedAtDesc(Quiz quiz);
}
