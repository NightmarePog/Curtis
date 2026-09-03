package com.sosehl.curtis.feature.subjects;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

    Optional<SubjectEntity> findByCodeIgnoreCase(String code);

    Optional<SubjectEntity> findByNameIgnoreCase(String name);

    List<SubjectEntity> findAllByOrderByNameAsc();

    List<SubjectEntity> findByIdInAndActiveTrueOrderByNameAsc(
        Collection<UUID> ids
    );
}
