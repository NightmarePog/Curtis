package com.sosehl.curtis.platform.security.domain;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public record CurrentUser(
    UUID id,
    String issuer,
    String subject,
    String username,
    String displayName,
    Set<UserRole> roles
) implements Serializable {
    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean isAdministrator() {
        return hasRole(UserRole.ADMINISTRATOR);
    }

    public String identityKey() {
        return issuer + '\n' + subject;
    }
}
