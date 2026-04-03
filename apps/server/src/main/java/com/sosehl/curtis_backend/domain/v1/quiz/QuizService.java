package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {

    private final QuizRepository repository;
    private final QuizMapper mapper;

    QuizService(QuizRepository repository, QuizMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public UUID createQuiz(QuizCreateRequest request) {
        Quiz quiz = mapper.toEntity(request);
        return repository.save(quiz).getUuid();
    }

    public QuizGetResponse returnQuiz(UUID uuid) {
        return repository
            .findByUuid(uuid)
            .map(mapper::toResponse)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Quiz not found"
                )
            );
    }

    public List<QuizGetResponse> returnAllQuizzes() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public void patchQuiz(QuizPatchRequest request, UUID uuid) {
        Quiz existing = repository
            .findByUuid(uuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Quiz not found"
                )
            );
        mapper.updateFromPatch(request, existing);
        repository.save(existing);
    }
}
