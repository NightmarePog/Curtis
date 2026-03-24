package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.mappers.QuizMapper;
import com.sosehl.curtis_backend.models.QuizModel;
import com.sosehl.curtis_backend.repositories.QuizRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

@Service
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

    public Optional<List<QuizGetResponse>> returnAllQuizzes() {
        return Optional.of(
            repository.findAll().stream().map(mapper::mapGetResponse).toList()
        );
    }

    public void patchQuiz(@RequestBody QuizPatchRequest quizPatch) {
        QuizModel existingQuiz = repository
            .findByUuid(quizPatch.getUuid())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Quiz not found"
                )
            );

        repository.save(mapper.mapPatchQuiz(quizPatch, existingQuiz));
    }
}
