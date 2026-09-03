package com.sosehl.curtis.feature.subjects;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectAssignmentRepository
    extends JpaRepository<SubjectAssignmentEntity, SubjectAssignmentId> {

    boolean existsByTeacherIdAndSubjectId(UUID teacherId, UUID subjectId);

    default boolean isAssigned(UUID teacherId, UUID subjectId) {
        return existsByTeacherIdAndSubjectId(teacherId, subjectId);
    }

    List<SubjectAssignmentEntity> findAllBySubjectId(UUID subjectId);

    List<SubjectAssignmentEntity> findAllByTeacherId(UUID teacherId);
}
