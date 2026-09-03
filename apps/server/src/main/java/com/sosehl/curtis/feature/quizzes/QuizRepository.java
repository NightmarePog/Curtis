package com.sosehl.curtis.feature.quizzes;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<QuizEntity, UUID> {
    List<QuizEntity> findAllByCreatorIdOrderByUpdatedAtDesc(UUID creatorId);
    List<QuizEntity> findAllByOrderByUpdatedAtDesc();

    boolean existsByCreatorIdAndQuestions_MediaId(
        UUID creatorId,
        UUID mediaId
    );
}
