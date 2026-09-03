package com.sosehl.curtis.feature.users.dto;

import com.sosehl.curtis.feature.users.core.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {}

    public record UpdateUserRequest(
        @NotNull Boolean active,
        @PositiveOrZero long version
    ) {}

    public record UserResponse(
        UUID id,
        String username,
        String displayName,
        Set<UserRole> roles,
        boolean active,
        Instant firstLoginAt,
        Instant lastLoginAt,
        long version
    ) {}
}
