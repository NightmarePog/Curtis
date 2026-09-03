package com.sosehl.curtis.platform.security.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntraRoleMappingTest {

    @Test
    void keepsMultipleKnownRolesAndIgnoresUnknownClaims() {
        assertThat(
            EntraRoleMapper.mapRoleNames(
                List.of("Teachers", "administrator", "unknown")
            )
        )
            .containsExactlyInAnyOrder(
                UserRole.TEACHER,
                UserRole.ADMINISTRATOR
            );
    }
}
