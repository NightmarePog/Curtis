package com.sosehl.curtis.feature.classrooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(ClassStudentAssignmentId.class)
@Table(name = "class_students")
public class ClassStudentAssignmentEntity {

    @Id
    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Id
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected ClassStudentAssignmentEntity() {}

    public ClassStudentAssignmentEntity(
        UUID classId,
        UUID studentId,
        UUID assignedBy,
        Instant assignedAt
    ) {
        this.classId = classId;
        this.studentId = studentId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public UUID classId() {
        return classId;
    }

    public UUID studentId() {
        return studentId;
    }
}
