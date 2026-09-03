package com.sosehl.curtis.feature.users.core;

import java.util.Set;
import java.util.UUID;

public record UserIdentity(
    UUID id,
    String username,
    String displayName,
    Set<UserRole> roles,
    boolean active
) {
    public UserIdentity {
        roles = Set.copyOf(roles);
    }
}
