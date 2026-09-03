package com.sosehl.curtis.platform.security.application;

import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserIdentity;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.shared.errors.ProblemException;

@Service
public class CurrentUserService {

    private final UserDirectory userDirectory;

    public CurrentUserService(UserDirectory userDirectory) {
        this.userDirectory = userDirectory;
    }

    public CurrentUser require(OidcUser oidcUser) {
        if (oidcUser == null) {
            throw ProblemException.unauthorized(
                "authentication_required",
                "A verified Microsoft sign-in is required."
            );
        }

        String issuer = issuer(oidcUser);
        UserIdentity account = userDirectory.requireIdentity(
            issuer,
            oidcUser.getSubject()
        );
        if (!account.active()) {
            throw ProblemException.forbidden(
                "user_inactive",
                "This user account is inactive."
            );
        }
        if (!authenticatedRoles(oidcUser.getAuthorities()).equals(account.roles())) {
            throw ProblemException.unauthorized(
                "roles_changed",
                "Your assigned roles changed. Sign in again to continue."
            );
        }

        return new CurrentUser(
            account.id(),
            issuer,
            oidcUser.getSubject(),
            account.username(),
            account.displayName(),
            account.roles()
        );
    }

    /** Resolves a session identity during logout without requiring database access. */
    public Optional<String> findIdentityKey(Authentication authentication) {
        if (
            authentication == null ||
            !(authentication.getPrincipal() instanceof OidcUser oidcUser) ||
            oidcUser.getIssuer() == null ||
            oidcUser.getSubject() == null ||
            oidcUser.getSubject().isBlank()
        ) {
            return Optional.empty();
        }
        return Optional.of(identityKey(
            oidcUser.getIssuer().toExternalForm(),
            oidcUser.getSubject()
        ));
    }

    private String issuer(OidcUser user) {
        URL issuer = user.getIssuer();
        if (issuer == null) {
            throw ProblemException.unauthorized(
                "invalid_identity",
                "The Microsoft identity has no issuer."
            );
        }
        return issuer.toExternalForm();
    }

    private String identityKey(String issuer, String subject) {
        return issuer + '\n' + subject;
    }

    private Set<UserRole> authenticatedRoles(
        Collection<? extends GrantedAuthority> authorities
    ) {
        Set<UserRole> roles = EnumSet.noneOf(UserRole.class);
        for (GrantedAuthority authority : authorities) {
            for (UserRole role : UserRole.values()) {
                if (("ROLE_" + role.name()).equals(authority.getAuthority())) {
                    roles.add(role);
                }
            }
        }
        return roles;
    }
}
