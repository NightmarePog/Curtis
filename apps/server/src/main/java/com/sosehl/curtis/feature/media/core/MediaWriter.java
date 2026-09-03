package com.sosehl.curtis.feature.media.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Stores media supplied by another Curtis feature. */
@FunctionalInterface
public interface MediaWriter {

    /** Stores and closes {@code input}. */
    UUID storeForImport(
        UUID ownerId,
        String originalName,
        InputStream input
    ) throws IOException;
}
