package com.sosehl.curtis.feature.classrooms;

import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.CreateClass;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.CreateGroup;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.UpdateClass;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.UpdateGroup;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomResponse;
import com.sosehl.curtis.platform.security.web.SOSE_AdminApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@SOSE_AdminApiController
@RequestMapping("/v1/admin/classes")
public class AdminClassroomController {

    private final ClassroomService service;

    public AdminClassroomController(ClassroomService service) {
        this.service = service;
    }

    @GetMapping
    List<ClassroomResponse> list(@SOSE_CurrentUser CurrentUser currentUser) {
        return service.listForAdministrator();
    }

    @GetMapping("/{classId}")
    ClassroomResponse get(
        @PathVariable UUID classId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.getForAdministrator(classId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClassroomResponse create(
        @Valid @RequestBody CreateClass request,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.createClass(
            request.name(),
            administrator.id()
        );
    }

    @PatchMapping("/{classId}")
    ClassroomResponse update(
        @PathVariable UUID classId,
        @Valid @RequestBody UpdateClass request,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.updateClass(
            classId,
            request.name(),
            request.active(),
            request.version()
        );
    }

    @PutMapping("/{classId}/teachers/{teacherId}")
    ClassroomResponse assignTeacher(
        @PathVariable UUID classId,
        @PathVariable UUID teacherId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.assignTeacher(
            classId,
            teacherId,
            administrator.id()
        );
    }

    @DeleteMapping("/{classId}/teachers/{teacherId}")
    ClassroomResponse removeTeacher(
        @PathVariable UUID classId,
        @PathVariable UUID teacherId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.removeTeacher(classId, teacherId);
    }

    @PutMapping("/{classId}/students/{studentId}")
    ClassroomResponse assignStudent(
        @PathVariable UUID classId,
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.assignStudent(
            classId,
            studentId,
            administrator.id()
        );
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    ClassroomResponse removeStudent(
        @PathVariable UUID classId,
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser currentUser
    ) {
        return service.removeStudent(classId, studentId);
    }

    @PostMapping("/{classId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    ClassroomResponse createGroup(
        @PathVariable UUID classId,
        @Valid @RequestBody CreateGroup request,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.createGroup(
            administrator.id(),
            true,
            classId,
            request.name()
        );
    }

    @PatchMapping("/{classId}/groups/{groupId}")
    ClassroomResponse updateGroup(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @Valid @RequestBody UpdateGroup request,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.updateGroup(
            administrator.id(),
            true,
            classId,
            groupId,
            request.name(),
            request.active(),
            request.version()
        );
    }

    @DeleteMapping("/{classId}/groups/{groupId}")
    ClassroomResponse archiveGroup(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.archiveGroup(
            administrator.id(),
            true,
            classId,
            groupId
        );
    }

    @PutMapping("/{classId}/groups/{groupId}/students/{studentId}")
    ClassroomResponse addGroupStudent(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.addGroupStudent(
            administrator.id(),
            true,
            classId,
            groupId,
            studentId
        );
    }

    @DeleteMapping("/{classId}/groups/{groupId}/students/{studentId}")
    ClassroomResponse removeGroupStudent(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser administrator
    ) {
        return service.removeGroupStudent(
            administrator.id(),
            true,
            classId,
            groupId,
            studentId
        );
    }
}
