package com.sosehl.curtis.feature.subjects;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class SubjectAssignmentId implements Serializable {

    private UUID teacherId;
    private UUID subjectId;

    public SubjectAssignmentId() {}

    public SubjectAssignmentId(UUID teacherId, UUID subjectId) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SubjectAssignmentId that)) {
            return false;
        }
        return (
            Objects.equals(teacherId, that.teacherId) &&
            Objects.equals(subjectId, that.subjectId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, subjectId);
    }
}
