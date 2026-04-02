package com.sosehl.curtis_backend.domain.v1.question;

import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionCreateDto;
import com.sosehl.curtis_backend.domain.v1.question.dto.QuestionPatchDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quiz/{quizUuid}/")
public class QuestionController {

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> create(
        @PathVariable("quizUuid") UUID quizUuid,
        @RequestBody @Valid QuestionCreateDto qDto
    ) {
        service.create(qDto, quizUuid);
        return ResponseEntity.ok("OK");
    }

    @GetMapping
    public ResponseEntity<List<Question>> getAll(
        @PathVariable("quizUuid") UUID quizUuid
    ) {
        List<Question> questions = service.getAll(quizUuid);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<Question> get(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId
    ) {
        Question question = service.get(quizUuid, questionId);
        return ResponseEntity.ok(question);
    }

    @PatchMapping("/{questionId}")
    public ResponseEntity<String> patch(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId,
        @RequestBody QuestionPatchDto dto
    ) {
        service.patch(dto, quizUuid);
        return ResponseEntity.ok("OK");
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<String> delete(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId
    ) {
        service.delete(quizUuid, questionId);
        return ResponseEntity.ok("OK");
    }
}
