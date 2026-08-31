package com.sosehl.curtis_backend.domain.v1.quiz;

import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizCreateRequest;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizGetResponse;
import com.sosehl.curtis_backend.domain.v1.quiz.dto.QuizPatchRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/quiz")
public class QuizController {

    private final QuizService service;
    private final QuizYamlImportService yamlImportService;

    QuizController(QuizService service, QuizYamlImportService yamlImportService) {
        this.service = service;
        this.yamlImportService = yamlImportService;
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
    public ResponseEntity<?> getAll(
        @RequestParam(required = false) boolean available,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        if (available) {
            return ResponseEntity.ok(service.returnAvailableQuizzes());
        }
        return ResponseEntity.ok(service.returnPagedQuizzes(pageable));
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

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        service.deleteQuiz(uuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, UUID>> importYaml(
        @RequestPart(value = "file", required = false) MultipartFile file,
        @RequestPart(value = "yaml", required = false) MultipartFile yaml,
        @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        MultipartFile source = file != null && !file.isEmpty() ? file : yaml;
        try {
            UUID uuid = yamlImportService.importUploaded(source, images);
            return ResponseEntity.created(URI.create("/v1/quiz/" + uuid)).body(
                Map.of("quizUuid", uuid)
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.getMessage() == null ? "YAML import selhal" : e.getMessage(),
                e
            );
        }
    }
}
