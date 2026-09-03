package com.sosehl.curtis.feature.users;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record UserAccountSnapshot(
    UUID id,
    String username,
    String displayName,
    Set<UserRole> roles,
    boolean active,
    Instant firstLoginAt,
    Instant lastLoginAt,
    long version
) {
    public UserAccountSnapshot {
        roles = Collections.unmodifiableSet(Set.copyOf(roles));
    }
}
