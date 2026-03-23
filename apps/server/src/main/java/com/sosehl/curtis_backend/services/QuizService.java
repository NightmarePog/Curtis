package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.models.QuestionsModel;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

public class QuizService {

    private final QuizRepository repository;
    private final QuizMapper mapper;

    QuizService(QuizRepository repository, QuizMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ResponseEntity<?> createQuiz(
        @RequestBody QuizCreateRequest quizCreate
    ) {
        repository.save(mapper.mapCreateQuiz(quizCreate));

        return ResponseEntity.ok().build();
    }
}
