package com.sosehl.curtis.feature.classrooms;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class ClassStudentAssignmentId implements Serializable {

    private UUID classId;
    private UUID studentId;

    public ClassStudentAssignmentId() {}

    public ClassStudentAssignmentId(UUID classId, UUID studentId) {
        this.classId = classId;
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassStudentAssignmentId that)) {
            return false;
        }
        return (
            Objects.equals(classId, that.classId) &&
            Objects.equals(studentId, that.studentId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, studentId);
    }
}
