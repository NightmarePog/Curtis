package com.sosehl.curtis.feature.media.core;

import java.util.UUID;

/** Lets another feature grant access without exposing media persistence. */
@FunctionalInterface
public interface MediaAccessContributor {
    boolean canRead(UUID userId, UUID mediaId);
}
