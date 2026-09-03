package com.sosehl.curtis.feature.classrooms;

import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.CreateGroup;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomRequests.UpdateGroup;
import com.sosehl.curtis.feature.classrooms.dto.ClassroomResponse;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
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

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/classes")
public class TeacherClassroomController {

    private final ClassroomService service;

    public TeacherClassroomController(ClassroomService service) {
        this.service = service;
    }

    @GetMapping
    List<ClassroomResponse> list(@SOSE_CurrentUser CurrentUser teacher) {
        return service.listForTeacher(teacher.id());
    }

    @GetMapping("/{classId}")
    ClassroomResponse get(
        @PathVariable UUID classId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.getForTeacher(teacher.id(), classId);
    }

    @PostMapping("/{classId}/groups")
    @ResponseStatus(HttpStatus.CREATED)
    ClassroomResponse createGroup(
        @PathVariable UUID classId,
        @Valid @RequestBody CreateGroup request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.createGroup(
            teacher.id(),
            false,
            classId,
            request.name()
        );
    }

    @PatchMapping("/{classId}/groups/{groupId}")
    ClassroomResponse updateGroup(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @Valid @RequestBody UpdateGroup request,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.updateGroup(
            teacher.id(),
            false,
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
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.archiveGroup(
            teacher.id(),
            false,
            classId,
            groupId
        );
    }

    @PutMapping("/{classId}/groups/{groupId}/students/{studentId}")
    ClassroomResponse addGroupStudent(
        @PathVariable UUID classId,
        @PathVariable UUID groupId,
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.addGroupStudent(
            teacher.id(),
            false,
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
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return service.removeGroupStudent(
            teacher.id(),
            false,
            classId,
            groupId,
            studentId
        );
    }

}
