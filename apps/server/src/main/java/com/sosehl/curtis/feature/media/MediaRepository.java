package com.sosehl.curtis.feature.media;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {}
