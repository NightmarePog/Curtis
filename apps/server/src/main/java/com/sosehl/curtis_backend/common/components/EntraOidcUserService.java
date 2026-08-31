package com.sosehl.curtis_backend.common.components;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class EntraOidcUserService
    implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final String testRole;
    private final String testUser;

    public EntraOidcUserService(
        @Value("${app.test-role:}") String testRole,
        @Value("${app.test-user:}") String testUser
    ) {
        this.testRole = testRole;
        this.testUser = testUser;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest)
        throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        List<String> roles = oidcUser
            .getIdToken()
            .getClaimAsStringList("roles");

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(
            oidcUser.getAuthorities()
        );
        authorities.addAll(EntraRoleMapper.mapRoles(roles));

        if (
            hasText(testRole) &&
            hasText(testUser) &&
            testUser.equalsIgnoreCase(oidcUser.getPreferredUsername())
        ) {
            authorities.addAll(EntraRoleMapper.mapRoles(List.of(testRole)));
        }

        String userNameAttributeName = userRequest
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();

        return new DefaultOidcUser(
            authorities,
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            userNameAttributeName != null ? userNameAttributeName : "sub"
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
