package com.sosehl.curtis_backend.repositories;

import com.sosehl.curtis_backend.models.QuizModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<QuizModel, Long> {
    Optional<QuizModel> findByUuid(UUID uuid);
}
