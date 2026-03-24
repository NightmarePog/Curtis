package com.sosehl.curtis_backend.controllers;

import com.sosehl.curtis_backend.dto.receive.QuizCreateRequest;
import com.sosehl.curtis_backend.dto.receive.QuizPatchRequest;
import com.sosehl.curtis_backend.dto.response.QuizGetResponse;
import com.sosehl.curtis_backend.services.QuizService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
class QuizController {

    private final QuizService service;

    QuizController(QuizService quizService) {
        this.service = quizService;
    }

    @PostMapping
    public ResponseEntity<?> create(
        @RequestBody @Valid QuizCreateRequest createRequest
    ) {
        service.createQuiz(createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<QuizGetResponse>> getAll() {
        return service
            .returnAllQuizzes()
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<?> get(@PathVariable UUID uuid) {
        return service
            .returnQuiz(uuid)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping
    public ResponseEntity<?> patch(
        @RequestBody @Valid QuizPatchRequest quizPatch
    ) {
        service.patchQuiz(quizPatch);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
