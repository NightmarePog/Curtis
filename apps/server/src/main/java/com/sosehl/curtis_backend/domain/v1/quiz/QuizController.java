package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/quiz")
public class QuizController {

    private final QuizService service;

    QuizController(QuizService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, UUID>> create(
        @RequestBody @Valid QuizCreateRequest request
    ) {
        UUID uuid = service.createQuiz(request);
        return ResponseEntity.created(URI.create("/v1/quiz/" + uuid)).body(
            Map.of("quizUuid", uuid)
        );
    }

    @GetMapping
    public ResponseEntity<List<QuizGetResponse>> getAll() {
        return ResponseEntity.ok(service.returnAllQuizzes());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<QuizGetResponse> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.returnQuiz(uuid));
    }

    @PatchMapping("/{uuid}")
    public ResponseEntity<Void> patch(
        @PathVariable UUID uuid,
        @RequestBody @Valid QuizPatchRequest request
    ) {
        service.patchQuiz(request, uuid);
        return ResponseEntity.ok().build();
    }
}
