package com.sosehl.curtis_backend.security;

import static org.assertj.core.api.Assertions.*;

import com.sosehl.curtis_backend.common.components.EntraRoleMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class EntraRoleMapperTest {

    @Test
    void shouldReturnEmptySetForNullInput() {
        assertThat(EntraRoleMapper.mapRoles(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptySetForEmptyInput() {
        assertThat(EntraRoleMapper.mapRoles(List.of())).isEmpty();
    }

    @Test
    void shouldMapTeacherRole() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("teachers")
        );
        assertThat(authorities).containsExactly(
            new SimpleGrantedAuthority("ROLE_TEACHER")
        );
    }

    @Test
    void shouldMapStudentRole() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("students")
        );
        assertThat(authorities).containsExactly(
            new SimpleGrantedAuthority("ROLE_STUDENT")
        );
    }

    @Test
    void shouldMapBothRolesWhenPresent() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("teachers", "students")
        );
        assertThat(authorities).containsExactlyInAnyOrder(
            new SimpleGrantedAuthority("ROLE_TEACHER"),
            new SimpleGrantedAuthority("ROLE_STUDENT")
        );
    }

    @Test
    void shouldIgnoreUnknownRoleValues() {
        Set<GrantedAuthority> authorities = EntraRoleMapper.mapRoles(
            List.of("SchoolAdmin")
        );
        assertThat(authorities).isEmpty();
    }
}
