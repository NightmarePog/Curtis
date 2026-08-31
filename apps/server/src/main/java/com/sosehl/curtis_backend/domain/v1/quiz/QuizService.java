package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import com.sosehl.curtis_backend.domain.v1.snapshot.SnapshotService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {

    private final QuizRepository repository;
    private final QuizMapper mapper;
    private final SnapshotService snapshotService;

    QuizService(QuizRepository repository, QuizMapper mapper, SnapshotService snapshotService) {
        this.repository = repository;
        this.mapper = mapper;
        this.snapshotService = snapshotService;
    }

    public UUID createQuiz(QuizCreateRequest request) {
        Quiz quiz = mapper.toEntity(request);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setEditedAt(LocalDateTime.now());
        if (quiz.getStatus() == null) {
            quiz.setStatus(QuizStatus.DRAFT);
        }
        repository.save(quiz);
        snapshotService.createSnapshotIfChanged(quiz);
        return quiz.getUuid();
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

    public Page<QuizGetResponse> returnPagedQuizzes(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public List<QuizGetResponse> returnAvailableQuizzes() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findAll().stream()
            .filter(q -> q.getStatus() == QuizStatus.RUNNING || q.getStatus() == null)
            .filter(q -> q.getValidFrom() == null || !q.getValidFrom().isAfter(now))
            .filter(q -> q.getValidTo() == null || !q.getValidTo().isBefore(now))
            .map(mapper::toResponse)
            .toList();
    }

    @Transactional
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
        existing.setEditedAt(LocalDateTime.now());
        repository.save(existing);
        snapshotService.createSnapshotIfChanged(existing);
    }

    @Transactional
    public void deleteQuiz(UUID uuid) {
        Quiz quiz = repository
            .findByUuid(uuid)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Quiz not found"
                )
            );
        repository.delete(quiz);
    }
}
