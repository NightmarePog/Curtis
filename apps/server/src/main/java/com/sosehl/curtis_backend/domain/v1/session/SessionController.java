package com.sosehl.curtis_backend.domain.v1.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quizResult.QuizResult;
import com.sosehl.curtis_backend.domain.v1.quizResult.ResultsResponse;
import com.sosehl.curtis_backend.domain.v1.session.dto.AwardPointsRequest;
import com.sosehl.curtis_backend.domain.v1.session.dto.PendingTextAnswerResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final SessionService service;
    private final ObjectMapper objectMapper;

    public SessionController(SessionService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<UUID> create(
        @RequestParam UUID quizUuid,
        @RequestParam(required = false) Integer expiresInMinutes,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        UUID sessionUuid = service.createSession(
            quizUuid,
            expiresInMinutes,
            principal
        );
        return ResponseEntity.created(
            URI.create("/v1/sessions/" + sessionUuid)
        ).body(sessionUuid);
    }

    @PostMapping("/{sessionUuid}/join")
    public ResponseEntity<QuestionResponse> join(
        @PathVariable UUID sessionUuid,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        return ResponseEntity.ok(service.join(sessionUuid, principal));
    }

    @PostMapping("/{sessionUuid}/next")
    public ResponseEntity<QuestionResponse> next(
        @PathVariable UUID sessionUuid,
        @RequestBody JsonNode answer,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        try {
            QuestionSubmission submission;
            if (answer.isArray()) {
                submission = QuestionSubmission.multipleChoice(
                    objectMapper.convertValue(
                        answer,
                        objectMapper
                            .getTypeFactory()
                            .constructCollectionType(List.class, Integer.class)
                    )
                );
            } else {
                submission = objectMapper.treeToValue(answer, QuestionSubmission.class);
            }
            return ResponseEntity.ok(service.next(sessionUuid, submission, principal));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Odpověď má neplatný formát",
                e
            );
        }
    }

    @GetMapping("/{sessionUuid}/results")
    public ResponseEntity<List<QuizResult>> results(
        @PathVariable UUID sessionUuid
    ) {
        return ResponseEntity.ok(service.getResults(sessionUuid));
    }

    @PostMapping("/{sessionUuid}/finish")
    public ResponseEntity<ResultsResponse> finish(
        @PathVariable UUID sessionUuid,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        return ResponseEntity.ok(service.finish(sessionUuid, principal));
    }

    @GetMapping("/{sessionUuid}/pending-text-answers")
    public ResponseEntity<List<PendingTextAnswerResponse>> pendingTextAnswers(
        @PathVariable UUID sessionUuid
    ) {
        return ResponseEntity.ok(service.getPendingTextAnswers(sessionUuid));
    }

    @PostMapping("/{sessionUuid}/text-answers/{resultId}/grade")
    public ResponseEntity<PendingTextAnswerResponse> gradeTextAnswer(
        @PathVariable UUID sessionUuid,
        @PathVariable Long resultId,
        @RequestBody @Valid AwardPointsRequest request
    ) {
        return ResponseEntity.ok(
            service.awardTextPoints(sessionUuid, resultId, request.getAwardedPoints())
        );
    }

    @GetMapping("/my-results")
    public ResponseEntity<List<QuizResult>> myResults(
        @AuthenticationPrincipal OAuth2User principal
    ) {
        String studentId = principal.getAttribute("sub");
        return ResponseEntity.ok(service.getStudentResults(studentId));
    }
}
