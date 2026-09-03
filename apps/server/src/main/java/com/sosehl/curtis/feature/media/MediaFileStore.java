package com.sosehl.curtis.feature.media;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.io.Resource;

/** Stores the binary content that belongs to persisted media metadata. */
interface MediaFileStore {

    record StoredFile(
        String storageKey,
        String originalName,
        String contentType,
        long byteSize,
        String sha256
    ) {}

    /**
     * Stores and closes {@code content}.
     */
    StoredFile store(
        UUID mediaId,
        String originalName,
        InputStream content
    ) throws IOException;

    Optional<Resource> find(String storageKey);

    void delete(String storageKey) throws IOException;
}
