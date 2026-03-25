package com.sosehl.curtis_backend.repositories;

import com.sosehl.curtis_backend.models.QuestionModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionModel, Long> {
    Optional<List<QuestionModel>> findByQuiz_Uuid(UUID quizUuid);
}
