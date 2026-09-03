package com.sosehl.curtis.feature.media.core;

import java.util.UUID;

/** Verifies that an actor may attach an existing image to owned content. */
@FunctionalInterface
public interface MediaUsagePolicy {
    void requireUsable(UUID mediaId, UUID ownerId, boolean administrator);
}
