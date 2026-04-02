package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
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

    QuizService(QuizRepository repository) {
        this.repository = repository;
    }

    public UUID createQuiz(@RequestBody QuizCreateRequest quizCreateDto) {
        Quiz quiz = QuizMapper.mapCreateQuiz(quizCreateDto);
        Quiz savedQuiz = repository.save(quiz);
        return savedQuiz.getUuid();
    }

    public Optional<QuizGetResponse> returnQuiz(UUID quizUuid) {
        return repository.findByUuid(quizUuid).map(QuizMapper::mapGetResponse);
    }

    public Optional<List<QuizGetResponse>> returnAllQuizzes() {
        return Optional.of(
            repository
                .findAll()
                .stream()
                .map(QuizMapper::mapGetResponse)
                .toList()
        );
    }

    public void patchQuiz(@RequestBody QuizPatchRequest quizPatch) {
        Quiz existingQuiz = repository
            .findByUuid(quizPatch.getUuid())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Quiz not found"
                )
            );

        repository.save(QuizMapper.mapPatchQuiz(quizPatch, existingQuiz));
    }
}
