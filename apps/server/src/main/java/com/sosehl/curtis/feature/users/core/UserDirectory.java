package com.sosehl.curtis.feature.users.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserDirectory {

    UserSummary recordLogin(
        String issuer,
        String subject,
        String username,
        String displayName,
        Set<UserRole> roles
    );

    UserIdentity requireIdentity(String issuer, String subject);

    UserSummary require(UUID id);

    List<UserSummary> summariesByIds(Collection<UUID> ids);

    Set<UUID> activeIdsWithRole(UserRole role);

    void requireRole(UUID userId, UserRole role);

    UserSummary requireActiveTeacher(UUID id);

    UserSummary requireActiveTeacherByUsername(String username);
}
