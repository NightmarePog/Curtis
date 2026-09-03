package com.sosehl.curtis.feature.sessions;

import com.sosehl.curtis.feature.sessions.attempt.AttemptQueryService;
import com.sosehl.curtis.feature.sessions.attempt.AttemptService;
import com.sosehl.curtis.feature.sessions.attempt.dto.AnswerRequest;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptDetailResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptResponse;
import com.sosehl.curtis.feature.sessions.attempt.dto.AttemptSummaryResponse;
import com.sosehl.curtis.feature.sessions.dto.SessionResponse;
import com.sosehl.curtis.feature.sessions.ranking.RankingService;
import com.sosehl.curtis.feature.sessions.ranking.dto.RankingResponse;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.web.SOSE_StudentApiController;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@SOSE_StudentApiController
@RequestMapping("/v1/student")
public class StudentSessionController {
    private final SessionService sessions;
    private final AttemptService attempts;
    private final AttemptQueryService queries;
    private final RankingService rankings;

    public StudentSessionController(
        SessionService sessions,
        AttemptService attempts,
        AttemptQueryService queries,
        RankingService rankings
    ) {
        this.sessions = sessions;
        this.attempts = attempts;
        this.queries = queries;
        this.rankings = rankings;
    }

    @GetMapping("/sessions")
    public List<SessionResponse> active(@SOSE_CurrentUser CurrentUser student) {
        return sessions.listForStudent(student.id());
    }

    @PostMapping("/sessions/{sessionId}/attempts")
    public ResponseEntity<AttemptResponse> start(
        @PathVariable UUID sessionId,
        @SOSE_CurrentUser CurrentUser student
    ) {
        AttemptResponse value = attempts.startOrResume(sessionId, student.id());
        return ResponseEntity.created(URI.create("/v1/student/attempts/" + value.id())).body(value);
    }

    @GetMapping("/attempts/{attemptId}")
    public AttemptResponse resume(
        @PathVariable UUID attemptId,
        @SOSE_CurrentUser CurrentUser student
    ) {
        return attempts.resume(attemptId, student.id());
    }

    @PutMapping("/attempts/{attemptId}/questions/{questionId}/answer")
    public AttemptResponse answer(
        @PathVariable UUID attemptId,
        @PathVariable UUID questionId,
        @RequestBody @Valid AnswerRequest request,
        @SOSE_CurrentUser CurrentUser student
    ) {
        return attempts.answer(
            attemptId, questionId, request.toCommand(), student.id()
        );
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptResponse submit(
        @PathVariable UUID attemptId,
        @SOSE_CurrentUser CurrentUser student
    ) {
        return attempts.submit(attemptId, student.id());
    }

    @GetMapping("/results")
    public List<AttemptSummaryResponse> results(
        @RequestParam(defaultValue = "100") int limit,
        @SOSE_CurrentUser CurrentUser student
    ) {
        return queries.listForStudent(student.id(), limit);
    }

    @GetMapping("/results/{attemptId}")
    public AttemptDetailResponse result(
        @PathVariable UUID attemptId,
        @SOSE_CurrentUser CurrentUser student
    ) {
        return queries.detailForStudent(attemptId, student.id());
    }

    @GetMapping("/rankings")
    public List<RankingResponse> rankings(@SOSE_CurrentUser CurrentUser student) {
        return rankings.forStudent(student.id());
    }
}
