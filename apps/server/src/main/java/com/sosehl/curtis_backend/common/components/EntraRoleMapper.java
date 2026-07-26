package com.sosehl.curtis_backend.common.components;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class EntraRoleMapper {

    private EntraRoleMapper() {}

    public static Set<GrantedAuthority> mapRoles(Collection<String> roles) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        if (roles == null) {
            return authorities;
        }

        for (String role : roles) {
            if ("Teacher".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));
            } else if ("Student".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            }
        }
        return authorities;
    }
}
