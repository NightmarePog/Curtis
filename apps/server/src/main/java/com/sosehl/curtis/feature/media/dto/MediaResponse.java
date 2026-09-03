package com.sosehl.curtis.feature.media.dto;

import java.util.UUID;

public record MediaResponse(
    UUID id,
    String originalName,
    String contentType,
    long byteSize,
    String sha256
) {}
