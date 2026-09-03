package com.sosehl.curtis.feature.yaml;

import com.sosehl.curtis.feature.quizzes.core.QuizAuthoring;
import com.sosehl.curtis.feature.yaml.dto.QuizIdResponse;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/quizzes")
public class TeacherYamlController {

    private static final MediaType YAML = new MediaType(
        "application",
        "yaml",
        StandardCharsets.UTF_8
    );

    private final YamlService yaml;

    TeacherYamlController(YamlService yaml) {
        this.yaml = yaml;
    }

    @PostMapping(
        value = "/yaml",
        consumes = { "application/yaml", "text/yaml", "text/plain" }
    )
    ResponseEntity<QuizIdResponse> create(
        @RequestBody byte[] body,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        QuizAuthoring.Quiz created = yaml.createForTeacher(
            teacher.id(),
            yaml.parse(body),
            Map.of()
        );
        return ResponseEntity
            .created(URI.create("/v1/teacher/quizzes/" + created.id()))
            .body(new QuizIdResponse(created.id(), created.version()));
    }

    @GetMapping(value = "/{quizId}/yaml", produces = "application/yaml")
    ResponseEntity<byte[]> export(
        @PathVariable UUID quizId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        byte[] body = yaml.exportForTeacher(
            teacher.id(),
            quizId
        );
        return ResponseEntity.ok()
            .contentType(YAML)
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=quiz-" + quizId + ".yaml"
            )
            .body(body);
    }

    @PutMapping(
        value = "/{quizId}/yaml",
        consumes = { "application/yaml", "text/yaml", "text/plain" }
    )
    QuizAuthoring.Quiz replace(
        @PathVariable UUID quizId,
        @RequestBody byte[] body,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return yaml.replaceForTeacher(
            teacher.id(),
            quizId,
            yaml.parse(body)
        );
    }
}
