package com.sosehl.curtis_backend.controllers;

import com.sosehl.curtis_backend.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.services.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
class QuizController {

    private final QuizService quizService;

    QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public ResponseEntity<?> createQuiz(
        @RequestBody @Valid QuizCreateRequest createRequest
    ) {
        return quizService.createQuiz(createRequest);
    }
}
