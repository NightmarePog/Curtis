package com.sosehl.curtis.feature.classrooms.core;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassroomDirectory {

    List<ClassroomRoster> activeForTeacher(UUID teacherId);

    Optional<ClassroomRoster> currentForStudent(UUID studentId);

    boolean isTeacherAssigned(UUID teacherId, UUID classId);
}
