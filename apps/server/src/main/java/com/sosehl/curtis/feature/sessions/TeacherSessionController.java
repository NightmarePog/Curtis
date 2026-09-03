package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.attempt.AttemptQueryService;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.dto.SessionCreateRequest;
import com.sosehl.curtis.feature.sessions.dto.SessionResponse;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/sessions")
public class TeacherSessionController {
    private final SessionService sessions;
    private final AttemptQueryService attempts;

    public TeacherSessionController(
        SessionService sessions,
        AttemptQueryService attempts
    ) {
        this.sessions = sessions;
        this.attempts = attempts;
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(
        @RequestBody @Valid SessionCreateRequest request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        SessionResponse created = sessions.create(
            request.toCommand(),
            teacher.id(),
            teacher.displayName()
        );
        return ResponseEntity.created(URI.create("/v1/teacher/sessions/" + created.id())).body(created);
    }

    @GetMapping
    public List<SessionResponse> list(@SOSE_CurrentUser CurrentUser teacher) {
        return sessions.listForTeacher(teacher.id());
    }

    @PostMapping("/{sessionId}/close")
    public SessionResponse close(
        @PathVariable UUID sessionId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return sessions.closeOwned(sessionId, teacher.id());
    }

    @GetMapping("/{sessionId}/attempts")
    public List<AttemptSummaryResponse> attempts(
        @PathVariable UUID sessionId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return attempts.listForTeacherSession(sessionId, teacher.id());
    }

    @GetMapping("/history")
    public List<AttemptSummaryResponse> history(
        @RequestParam(defaultValue = "100") int limit,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return attempts.historyForTeacher(teacher.id(), limit);
    }
}
