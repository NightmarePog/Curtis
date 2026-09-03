package com.sosehl.curtis.feature.quizzes;

import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.AdminQuizCreateRequest;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizIdResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizResponse;
import com.sosehl.curtis.feature.quizzes.dto.QuizDtos.QuizWriteRequest;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_AdminApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
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

@SOSE_AdminApiController
@RequestMapping("/v1/admin/quizzes")
public class AdminQuizController {

    private final QuizService quizzes;
    private final UserDirectory users;

    AdminQuizController(
        QuizService quizzes,
        UserDirectory users
    ) {
        this.quizzes = quizzes;
        this.users = users;
    }

    @PostMapping
    ResponseEntity<QuizIdResponse> create(
        @Valid @RequestBody AdminQuizCreateRequest request,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        users.requireActiveTeacher(request.creatorId());
        QuizResponse created = quizzes.createByAdministrator(
            request.creatorId(),
            request.quiz()
        );
        return ResponseEntity
            .created(URI.create("/v1/admin/quizzes/" + created.id()))
            .body(new QuizIdResponse(created.id(), created.version()));
    }

    @GetMapping
    List<QuizResponse> list(@SOSE_CurrentUser CurrentUser currentUser) {
        return quizzes.listForAdministrator();
    }

    @GetMapping("/{quizId}")
    QuizResponse get(
        @PathVariable UUID quizId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return quizzes.getForAdministrator(quizId);
    }

    @PutMapping("/{quizId}")
    QuizResponse replace(
        @PathVariable UUID quizId,
        @Valid @RequestBody QuizWriteRequest request,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return quizzes.replaceByAdministrator(quizId, request);
    }

    @DeleteMapping("/{quizId}")
    ResponseEntity<Void> archive(
        @PathVariable UUID quizId,
        @RequestParam long expectedVersion,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        quizzes.archiveByAdministrator(quizId, expectedVersion);
        return ResponseEntity.noContent().build();
    }
}
