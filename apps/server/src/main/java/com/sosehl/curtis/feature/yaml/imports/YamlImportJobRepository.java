package com.sosehl.curtis.feature.yaml.imports;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YamlImportJobRepository extends JpaRepository<YamlImportJobEntity, UUID> {
    Optional<YamlImportJobEntity> findByContentDigest(String contentDigest);
}
