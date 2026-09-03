package com.sosehl.curtis.feature.classrooms;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class ClassTeacherAssignmentId implements Serializable {

    private UUID classId;
    private UUID teacherId;

    public ClassTeacherAssignmentId() {}

    public ClassTeacherAssignmentId(UUID classId, UUID teacherId) {
        this.classId = classId;
        this.teacherId = teacherId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassTeacherAssignmentId that)) {
            return false;
        }
        return (
            Objects.equals(classId, that.classId) &&
            Objects.equals(teacherId, that.teacherId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, teacherId);
    }
}
