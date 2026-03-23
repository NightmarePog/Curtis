package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;

public class QuizService {

    private final QuizRepository repository;
    private final QuizMapper mapper;

    QuizService(QuizRepository repository, QuizMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void createQuiz(@RequestBody QuizCreateRequest quizCreateDto) {
        repository.save(mapper.mapCreateQuiz(quizCreateDto));
    }

    public Optional<QuizGetResponse> returnQuiz(UUID quizUuid) {
        return repository.findByUuid(quizUuid).map(mapper::mapGetResponse);
    }
}
