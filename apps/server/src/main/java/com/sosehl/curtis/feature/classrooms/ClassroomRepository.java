package com.sosehl.curtis.feature.classrooms;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomRepository
    extends JpaRepository<ClassroomEntity, UUID> {

    List<ClassroomEntity> findAllByOrderByNameAsc();

    List<ClassroomEntity> findAllByIdInAndActiveTrueOrderByNameAsc(
        Collection<UUID> ids
    );

    Optional<ClassroomEntity> findByNameIgnoreCase(String name);

    boolean existsByIdAndActiveTrue(UUID id);
}
