package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.attempt.AttemptQueryService;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptDetailResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.GradeRequest;
import com.sosehl.curtis.feature.sessions.attempt.dto.QuestionResultResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.ReviewResponse;
import com.sosehl.curtis.feature.sessions.dto.SessionResponse;
import com.sosehl.curtis.platform.security.web.SOSE_AdminApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@SOSE_AdminApiController
@RequestMapping("/v1/admin")
public class AdminSessionController {
    private final SessionService sessions;
    private final AttemptQueryService attempts;

    public AdminSessionController(
        SessionService sessions,
        AttemptQueryService attempts
    ) {
        this.sessions = sessions;
        this.attempts = attempts;
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return sessions.listForAdministrator();
    }

    @PostMapping("/sessions/{sessionId}/close")
    public SessionResponse close(
        @PathVariable UUID sessionId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return sessions.closeAsAdministrator(sessionId);
    }

    @GetMapping("/sessions/{sessionId}/attempts")
    public List<AttemptSummaryResponse> attempts(
        @PathVariable UUID sessionId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return attempts.listForAdminSession(sessionId);
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptDetailResponse detail(
        @PathVariable UUID attemptId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return attempts.detailForAdmin(attemptId);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> reviews(
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return attempts.pendingForAdmin();
    }

    @PutMapping("/reviews/{questionResultId}")
    public QuestionResultResponse grade(
        @PathVariable UUID questionResultId,
        @RequestBody @Valid GradeRequest request,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return attempts.gradeForAdmin(
            questionResultId,
            request.points(),
            administrator.id()
        );
    }
}
