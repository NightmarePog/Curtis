package com.sosehl.curtis.feature.subjects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(SubjectAssignmentId.class)
@Table(name = "teacher_subjects")
public class SubjectAssignmentEntity {

    @Id
    @Column(name = "teacher_id")
    UUID teacherId;

    @Id
    @Column(name = "subject_id")
    UUID subjectId;

    @Column(name = "assigned_by")
    UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    Instant assignedAt;

    protected SubjectAssignmentEntity() {}

    public SubjectAssignmentEntity(
        UUID teacherId,
        UUID subjectId,
        UUID assignedBy,
        Instant assignedAt
    ) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public UUID teacherId() {
        return teacherId;
    }

    public UUID subjectId() {
        return subjectId;
    }
}
