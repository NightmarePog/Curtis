package com.sosehl.curtis_backend.domain.v1.quizResult;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizResultRepository
    extends JpaRepository<QuizResult, Long> {

    List<QuizResult> findBySessionUuid(UUID sessionUuid);
}
