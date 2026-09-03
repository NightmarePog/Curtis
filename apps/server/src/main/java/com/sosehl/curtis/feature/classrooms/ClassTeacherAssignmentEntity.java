package com.sosehl.curtis.feature.classrooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(ClassTeacherAssignmentId.class)
@Table(name = "class_teachers")
public class ClassTeacherAssignmentEntity {

    @Id
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Id
    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected ClassTeacherAssignmentEntity() {}

    public ClassTeacherAssignmentEntity(
        UUID classId,
        UUID teacherId,
        UUID assignedBy,
        Instant assignedAt
    ) {
        this.classId = classId;
        this.teacherId = teacherId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public UUID classId() {
        return classId;
    }

    public UUID teacherId() {
        return teacherId;
    }
}
