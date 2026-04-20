package com.sosehl.curtis_backend.domain.v1.question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    Optional<List<Question>> findByQuiz_Uuid(UUID quizUuid);
}
