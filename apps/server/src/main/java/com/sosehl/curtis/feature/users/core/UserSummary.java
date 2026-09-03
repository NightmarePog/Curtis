package com.sosehl.curtis.feature.users.core;

import java.util.Set;
import java.util.UUID;
public record UserSummary(
    UUID id,
    String displayName,
    String username,
    Set<UserRole> roles
) {
    public UserSummary {
        roles = Set.copyOf(roles);
    }
}
