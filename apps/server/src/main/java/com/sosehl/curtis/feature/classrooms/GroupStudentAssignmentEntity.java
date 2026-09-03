package com.sosehl.curtis.feature.classrooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@IdClass(GroupStudentAssignmentId.class)
@Table(name = "group_students")
public class GroupStudentAssignmentEntity {

    @Id
    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Id
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    protected GroupStudentAssignmentEntity() {}

    public GroupStudentAssignmentEntity(
        UUID groupId,
        UUID classId,
        UUID studentId,
        UUID assignedBy,
        Instant assignedAt
    ) {
        this.groupId = groupId;
        this.classId = classId;
        this.studentId = studentId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public UUID groupId() {
        return groupId;
    }

    public UUID classId() {
        return classId;
    }

    public UUID studentId() {
        return studentId;
    }
}
