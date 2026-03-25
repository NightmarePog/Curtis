package com.sosehl.curtis_backend.controllers;

import com.sosehl.curtis_backend.dto.receive.QuestionCreateDto;
import com.sosehl.curtis_backend.dto.receive.QuestionPatchDto;
import com.sosehl.curtis_backend.models.QuestionModel;
import com.sosehl.curtis_backend.services.QuestionService;
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
    public ResponseEntity<?> create(
        @PathVariable("quizUuid") UUID quizUuid,
        @RequestBody @Valid QuestionCreateDto qDto
    ) {
        service.create(qDto, quizUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<QuestionModel>> getAll(
        @PathVariable("quizUuid") UUID quizUuid
    ) {
        List<QuestionModel> questions = service.getAll(quizUuid);
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<QuestionModel> get(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId
    ) {
        QuestionModel question = service.get(quizUuid, questionId);
        return ResponseEntity.ok(question);
    }

    @PatchMapping("/{questionId}")
    public ResponseEntity<?> patch(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId,
        @RequestBody QuestionPatchDto dto
    ) {
        // mapujeme questionId na originalOrder
        dto.setOriginalOrder(questionId);
        service.patch(dto, quizUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> delete(
        @PathVariable("quizUuid") UUID quizUuid,
        @PathVariable("questionId") Integer questionId
    ) {
        service.delete(quizUuid, questionId);
        return ResponseEntity.ok().build();
    }
}
