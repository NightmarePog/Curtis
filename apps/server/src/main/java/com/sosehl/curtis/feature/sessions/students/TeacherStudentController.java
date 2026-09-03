package com.sosehl.curtis.feature.sessions.students;

import com.sosehl.curtis.feature.sessions.students.dto.TeacherClassStudentsResponse;
import com.sosehl.curtis.feature.sessions.students.dto.TeacherStudentProfileResponse;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/students")
public class TeacherStudentController {
    private final TeacherStudentQueryService students;

    public TeacherStudentController(TeacherStudentQueryService students) {
        this.students = students;
    }

    @GetMapping
    public List<TeacherClassStudentsResponse> list(
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return students.list(teacher.id());
    }

    @GetMapping("/{studentId}")
    public TeacherStudentProfileResponse profile(
        @PathVariable UUID studentId,
        @SOSE_CurrentUser CurrentUser teacher
    ) {
        return students.profile(teacher.id(), studentId);
    }
}
