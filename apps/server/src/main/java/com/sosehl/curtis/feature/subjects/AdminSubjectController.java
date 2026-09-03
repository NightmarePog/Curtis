package com.sosehl.curtis.feature.subjects;

import com.sosehl.curtis.feature.subjects.dto.SubjectRequests.CreateSubjectRequest;
import com.sosehl.curtis.feature.subjects.dto.SubjectRequests.UpdateSubjectRequest;
import com.sosehl.curtis.feature.subjects.dto.SubjectResponse;
import com.sosehl.curtis.platform.security.web.SOSE_AdminApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@SOSE_AdminApiController
@RequestMapping("/v1/admin/subjects")
public class AdminSubjectController {

    private final SubjectService service;

    public AdminSubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping
    List<SubjectResponse> list(@SOSE_CurrentUser CurrentUser currentUser) {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SubjectResponse create(
        @Valid @RequestBody CreateSubjectRequest request,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.create(request.code(), request.name());
    }

    @PatchMapping("/{subjectId}")
    SubjectResponse update(
        @PathVariable UUID subjectId,
        @Valid @RequestBody UpdateSubjectRequest request,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.update(
            subjectId,
            request.code(),
            request.name(),
            request.active(),
            request.version()
        );
    }

    @PutMapping("/{subjectId}/teachers/{teacherId}")
    SubjectResponse assignTeacher(
        @PathVariable UUID subjectId,
        @PathVariable UUID teacherId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.assignTeacher(
            subjectId,
            teacherId,
            administrator.id()
        );
    }

    @DeleteMapping("/{subjectId}/teachers/{teacherId}")
    SubjectResponse unassignTeacher(
        @PathVariable UUID subjectId,
        @PathVariable UUID teacherId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.unassignTeacher(subjectId, teacherId);
    }

}
