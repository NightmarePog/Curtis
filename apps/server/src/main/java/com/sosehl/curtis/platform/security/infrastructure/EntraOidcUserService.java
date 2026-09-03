package com.sosehl.curtis.platform.security.infrastructure;

import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserRole;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private final UserDirectory userDirectory;

    public EntraOidcUserService(UserDirectory userDirectory) {
        this.userDirectory = userDirectory;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request)
        throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);
        Set<UserRole> roles = EntraRoleMapper.mapRoleNames(
            oidcUser.getIdToken().getClaimAsStringList("roles")
        );

        userDirectory.recordLogin(
            oidcUser.getIssuer().toExternalForm(),
            oidcUser.getSubject(),
            username(oidcUser),
            displayName(oidcUser),
            roles
        );

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(
            oidcUser.getAuthorities()
        );
        authorities.addAll(EntraRoleMapper.authorities(roles));
        String nameAttribute = request
            .getClientRegistration()
            .getProviderDetails()
            .getUserInfoEndpoint()
            .getUserNameAttributeName();
        return new DefaultOidcUser(
            authorities,
            oidcUser.getIdToken(),
            oidcUser.getUserInfo(),
            hasText(nameAttribute) ? nameAttribute : "sub"
        );
    }

    private String username(OidcUser user) {
        if (hasText(user.getPreferredUsername())) {
            return user.getPreferredUsername();
        }
        if (hasText(user.getEmail())) {
            return user.getEmail();
        }
        return user.getSubject();
    }

    private String displayName(OidcUser user) {
        return hasText(user.getFullName())
            ? user.getFullName()
            : username(user);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
