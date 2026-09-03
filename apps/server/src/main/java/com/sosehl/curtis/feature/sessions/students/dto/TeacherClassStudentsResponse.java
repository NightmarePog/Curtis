package com.sosehl.curtis.feature.sessions.students.dto;

import java.util.List;
import java.util.UUID;

public record TeacherClassStudentsResponse(
    UUID classId,
    String className,
    List<TeacherStudentResponse> students
) {}
