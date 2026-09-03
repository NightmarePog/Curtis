package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizIdResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/quizzes")
public class TeacherQuizController {

    private final QuizService quizzes;

    TeacherQuizController(QuizService quizzes) {
        this.quizzes = quizzes;
    }

    @PostMapping
    ResponseEntity<QuizIdResponse> create(
        @Valid @RequestBody QuizWriteRequest request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        QuizResponse created = quizzes.createForTeacher(teacher.id(), request);
        return ResponseEntity
            .created(URI.create("/v1/teacher/quizzes/" + created.id()))
            .body(new QuizIdResponse(created.id(), created.version()));
    }

    @GetMapping
    List<QuizResponse> list(@SOSE_CurrentUser CurrentUser teacher) {
        return quizzes.listForTeacher(teacher.id());
    }

    @GetMapping("/{quizId}")
    QuizResponse get(
        @PathVariable UUID quizId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return quizzes.getForTeacher(
            teacher.id(),
            quizId
        );
    }

    @PutMapping("/{quizId}")
    QuizResponse replace(
        @PathVariable UUID quizId,
        @Valid @RequestBody QuizWriteRequest request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return quizzes.replaceForTeacher(
            teacher.id(),
            quizId,
            request
        );
    }

    @DeleteMapping("/{quizId}")
    ResponseEntity<Void> archive(
        @PathVariable UUID quizId,
        @RequestParam long expectedVersion,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        quizzes.archiveForTeacher(
            teacher.id(),
            quizId,
            expectedVersion
        );
        return ResponseEntity.noContent().build();
    }
}
