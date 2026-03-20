package com.sosehl.curtis_backend.services;

import com.sosehl.curtis_backend.dto.QuizCreateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class QuizService {

    public ResponseEntity<?> createQuiz(
        @RequestBody QuizCreateRequest quizCreate
    ) {
        return ResponseEntity.ok().build();
    }
}
