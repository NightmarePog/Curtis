package com.sosehl.curtis.feature.classrooms;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupStudentAssignmentRepository
    extends JpaRepository<GroupStudentAssignmentEntity, GroupStudentAssignmentId> {

    boolean existsByGroupIdAndStudentId(UUID groupId, UUID studentId);

    List<GroupStudentAssignmentEntity> findAllByGroupIdIn(
        Collection<UUID> groupIds
    );

    List<GroupStudentAssignmentEntity> findAllByGroupIdOrderByStudentIdAsc(
        UUID groupId
    );

    void deleteByGroupIdAndStudentId(UUID groupId, UUID studentId);

    void deleteAllByGroupId(UUID groupId);
}
