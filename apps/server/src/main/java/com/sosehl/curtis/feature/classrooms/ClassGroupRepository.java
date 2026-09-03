package com.sosehl.curtis.feature.classrooms;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassGroupRepository
    extends JpaRepository<ClassGroupEntity, UUID> {

    List<ClassGroupEntity> findAllByClassIdOrderByNameAsc(UUID classId);

    List<ClassGroupEntity> findAllByClassIdInAndActiveTrueOrderByNameAsc(
        Collection<UUID> classIds
    );

    List<ClassGroupEntity> findAllByIdInAndActiveTrueOrderByNameAsc(
        Collection<UUID> ids
    );

    Optional<ClassGroupEntity> findByClassIdAndNameIgnoreCase(
        UUID classId,
        String name
    );
}
