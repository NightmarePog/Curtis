package com.sosehl.curtis.platform.security.infrastructure;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class EntraRoleMapper {

    private EntraRoleMapper() {}

    public static Set<UserRole> mapRoleNames(Collection<String> roleNames) {
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        if (roleNames == null) {
            return roles;
        }
        for (String roleName : roleNames) {
            UserRole role = mapRoleName(roleName);
            if (role != null) {
                roles.add(role);
            }
        }
        return roles;
    }

    public static Set<GrantedAuthority> authorities(Set<UserRole> roles) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        roles.forEach(role ->
            authorities.add(
                new SimpleGrantedAuthority("ROLE_" + role.name())
            )
        );
        return authorities;
    }

    private static UserRole mapRoleName(String roleName) {
        if (roleName == null) {
            return null;
        }
        return switch (roleName.trim().toLowerCase(Locale.ROOT)) {
            case "administrator", "administrators", "admin" ->
                UserRole.ADMINISTRATOR;
            case "teacher", "teachers" -> UserRole.TEACHER;
            case "student", "students" -> UserRole.STUDENT;
            default -> null;
        };
    }
}
