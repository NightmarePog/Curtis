package com.sosehl.curtis.feature.users.core;

import java.util.Objects;
import java.util.UUID;

/** Published when an account must lose access as soon as its transaction commits. */
public record UserAccessRevoked(UUID userId) {
    public UserAccessRevoked {
        Objects.requireNonNull(userId);
    }
}

