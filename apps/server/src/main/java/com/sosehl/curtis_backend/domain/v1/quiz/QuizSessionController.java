package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionAnswerDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.session.ResultsResponse;
import com.sosehl.curtis_backend.domain.v1.session.SessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuizSessionController {

    private SessionService service;

    QuizSessionController(SessionService service) {
        this.service = service;
    }

    @GetMapping("/session/start/{quizUuid}")
    public ResponseEntity<QuestionResponse> start(
        @PathVariable UUID quizUuid,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        QuestionResponse response = service.start(quizUuid, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/session/{sessionUuid}/next")
    public ResponseEntity<QuestionResponse> nextAction(
        @PathVariable UUID sessionUuid,
        @RequestBody @Valid QuestionAnswerDto answerDto
    ) {
        QuestionResponse nextQuestion = service.next(
            sessionUuid,
            answerDto.getQuestionIndexes()
        );
        if (nextQuestion == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(nextQuestion);
    }

    @PostMapping("/session/{sessionUuid}/results")
    public ResponseEntity<ResultsResponse> getResults(
        @PathVariable UUID sessionUuid
    ) {
        return ResponseEntity.ok((service.getResults(sessionUuid)));
    }
}
