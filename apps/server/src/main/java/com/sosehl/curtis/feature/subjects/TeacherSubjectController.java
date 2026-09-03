package com.sosehl.curtis.feature.subjects;

import com.sosehl.curtis.feature.subjects.core.SubjectSummary;
import com.sosehl.curtis.platform.security.web.SOSE_TeacherApiController;
import com.sosehl.curtis.platform.security.web.SOSE_CurrentUser;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@SOSE_TeacherApiController
@RequestMapping("/v1/teacher/subjects")
public class TeacherSubjectController {

    private final SubjectAccessService subjectAccess;

    public TeacherSubjectController(SubjectAccessService subjectAccess) {
        this.subjectAccess = subjectAccess;
    }

    @GetMapping
    List<SubjectSummary> list(@SOSE_CurrentUser CurrentUser teacher) {
        return subjectAccess.listForTeacher(teacher.id());
    }
}
