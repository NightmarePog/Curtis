package com.sosehl.curtis.feature.classrooms;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassStudentAssignmentRepository
    extends JpaRepository<ClassStudentAssignmentEntity, ClassStudentAssignmentId> {

    boolean existsByClassIdAndStudentId(UUID classId, UUID studentId);

    Optional<ClassStudentAssignmentEntity> findByStudentId(UUID studentId);

    List<ClassStudentAssignmentEntity> findAllByClassIdIn(
        Collection<UUID> classIds
    );

    List<ClassStudentAssignmentEntity> findAllByClassIdOrderByStudentIdAsc(
        UUID classId
    );

    void deleteAllByClassId(UUID classId);

    void deleteByClassIdAndStudentId(UUID classId, UUID studentId);
}
