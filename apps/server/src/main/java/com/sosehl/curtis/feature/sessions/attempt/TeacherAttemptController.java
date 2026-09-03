package com.sosehl.curtis.feature.sessions.attempt;

import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptDetailResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.GradeRequest;
import com.sosehl.curtis.feature.sessions.attempt.dto.QuestionResultResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.ReviewResponse;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher")
public class TeacherAttemptController {
    private final AttemptQueryService attempts;

    public TeacherAttemptController(AttemptQueryService attempts) {
        this.attempts = attempts;
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDetailResponse detail(
        @PathVariable UUID attemptId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return attempts.detailForTeacher(attemptId, teacher.id());
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(@SOSE_CurrentUser CurrentUser teacher) {
        return attempts.pendingForTeacher(teacher.id());
    }

    @PutMapping("/reviews/{questionResultId}")
    public QuestionResultResponse grade(
        @PathVariable UUID questionResultId,
        @RequestBody @Valid GradeRequest request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return attempts.gradeForTeacher(
            questionResultId,
            request.points(),
            teacher.id()
        );
    }
}
