package com.sosehl.curtis.platform.security.api.dto;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
    UUID id,
    String subject,
    String username,
    String displayName,
    Set<UserRole> roles
) {}
