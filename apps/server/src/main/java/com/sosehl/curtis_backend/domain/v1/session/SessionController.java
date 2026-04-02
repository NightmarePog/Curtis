package com.sosehl.curtis_backend.domain.v1.session;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionResponse;
import com.sosehl.curtis_backend.domain.v1.quizResult.ResultsResponse;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/sessions")
public class SessionController {

    private final SessionService service;

    public SessionController(SessionService service) {
        this.service = service;
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
            URI.create("/api/v1/sessions/" + sessionUuid)
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
        @RequestBody List<Integer> answer,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        return ResponseEntity.ok(service.next(sessionUuid, answer, principal));
    }

    @PostMapping("/{sessionUuid}/finish")
    public ResponseEntity<ResultsResponse> finish(
        @PathVariable UUID sessionUuid,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        return ResponseEntity.ok(service.finish(sessionUuid, principal));
    }
}
