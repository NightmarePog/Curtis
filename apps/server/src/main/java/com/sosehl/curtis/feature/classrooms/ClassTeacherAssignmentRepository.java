package com.sosehl.curtis.feature.classrooms;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassTeacherAssignmentRepository
    extends JpaRepository<ClassTeacherAssignmentEntity, ClassTeacherAssignmentId> {

    boolean existsByClassIdAndTeacherId(UUID classId, UUID teacherId);

    List<ClassTeacherAssignmentEntity> findAllByTeacherId(UUID teacherId);

    List<ClassTeacherAssignmentEntity> findAllByTeacherIdAndClassIdIn(
        UUID teacherId,
        Collection<UUID> classIds
    );

    List<ClassTeacherAssignmentEntity> findAllByClassIdOrderByTeacherIdAsc(
        UUID classId
    );

    void deleteByClassIdAndTeacherId(UUID classId, UUID teacherId);
}
