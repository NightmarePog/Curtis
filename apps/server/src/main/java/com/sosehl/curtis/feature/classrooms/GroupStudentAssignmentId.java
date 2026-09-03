package com.sosehl.curtis.feature.classrooms;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class GroupStudentAssignmentId implements Serializable {

    private UUID groupId;
    private UUID studentId;

    public GroupStudentAssignmentId() {}

    public GroupStudentAssignmentId(UUID groupId, UUID studentId) {
        this.groupId = groupId;
        this.studentId = studentId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GroupStudentAssignmentId that)) {
            return false;
        }
        return (
            Objects.equals(groupId, that.groupId) &&
            Objects.equals(studentId, that.studentId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, studentId);
    }
}
