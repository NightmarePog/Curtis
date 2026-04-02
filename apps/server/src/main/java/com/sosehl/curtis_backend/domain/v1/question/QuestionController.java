package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionPatchDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/quizzes/{quizUuid}/questions")
public class QuestionController {

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> create(
        @PathVariable UUID quizUuid,
        @RequestBody @Valid QuestionCreateDto dto
    ) {
        service.create(dto, quizUuid);
        return ResponseEntity.created(
            URI.create("/api/v1/quizzes/" + quizUuid + "/questions")
        ).build();
    }

    @GetMapping
    public ResponseEntity<List<Question>> getAll(@PathVariable UUID quizUuid) {
        return ResponseEntity.ok(service.getAll(quizUuid));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<Question> get(
        @PathVariable UUID quizUuid,
        @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(service.get(quizUuid, questionId));
    }

    @PatchMapping("/{questionId}")
    public ResponseEntity<Void> patch(
        @PathVariable UUID quizUuid,
        @PathVariable Long questionId,
        @RequestBody @Valid QuestionPatchDto dto
    ) {
        dto.setQuestionId(questionId);
        service.patch(dto, quizUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> delete(
        @PathVariable UUID quizUuid,
        @PathVariable Long questionId
    ) {
        service.delete(quizUuid, questionId);
        return ResponseEntity.noContent().build();
    }
}
