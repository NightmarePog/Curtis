package com.sosehl.curtis_backend.repositories;

import com.sosehl.curtis_backend.models.QuizModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<QuizModel, Long> {}
