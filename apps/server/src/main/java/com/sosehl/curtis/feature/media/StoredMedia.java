package com.sosehl.curtis.feature.media;

import java.util.UUID;
import org.springframework.core.io.Resource;

public record StoredMedia(
    UUID id,
    String originalName,
    String contentType,
    long byteSize,
    String sha256,
    Resource content
) {}
