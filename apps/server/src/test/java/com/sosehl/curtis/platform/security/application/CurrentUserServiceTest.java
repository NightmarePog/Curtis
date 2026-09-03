package com.sosehl.curtis.platform.security.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserIdentity;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.platform.security.domain.CurrentUser;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final UUID USER_ID = UUID.fromString(
        "2251ac6d-589d-4bb4-9794-499b8771fdb1"
    );
    private static final String ISSUER = "https://login.example.test/tenant";
    private static final String SUBJECT = "entra-subject";

    @Mock
    private UserDirectory userDirectory;

    @Mock
    private OidcUser oidcUser;

    private CurrentUserService currentUsers;

    @BeforeEach
    void setUp() throws Exception {
        currentUsers = new CurrentUserService(userDirectory);
        when(oidcUser.getIssuer()).thenReturn(new URL(ISSUER));
        when(oidcUser.getSubject()).thenReturn(SUBJECT);
    }

    @Test
    void returnsCurrentDatabaseSnapshotWhenRecognizedRolesMatch() {
        Set<UserRole> roles = Set.of(UserRole.TEACHER, UserRole.STUDENT);
        account(roles, true);
        authorities(
            "OIDC_USER",
            "SCOPE_openid",
            "ROLE_TEACHER",
            "ROLE_STUDENT",
            "ROLE_UNKNOWN"
        );

        CurrentUser current = currentUsers.require(oidcUser);

        assertThat(current.id()).isEqualTo(USER_ID);
        assertThat(current.roles()).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    void rejectsSessionWhenAPreviouslyAuthenticatedRoleWasRemoved() {
        account(Set.of(UserRole.TEACHER), true);
        authorities("ROLE_TEACHER", "ROLE_STUDENT");

        assertRolesChanged();
    }

    @Test
    void rejectsSessionWhenARecognizedRoleWasAddedAfterAuthentication() {
        account(Set.of(UserRole.TEACHER, UserRole.ADMINISTRATOR), true);
        authorities("ROLE_TEACHER");

        assertRolesChanged();
    }

    @Test
    void inactiveAccountTakesPrecedenceOverAChangedRoleSnapshot() {
        account(Set.of(UserRole.TEACHER), false);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
            .when(oidcUser)
            .getAuthorities();

        assertThatExceptionOfType(ProblemException.class)
            .isThrownBy(() -> currentUsers.require(oidcUser))
            .satisfies(problem -> {
                assertThat(problem.status()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(problem.code()).isEqualTo("user_inactive");
                assertThat(problem.getMessage()).isEqualTo(
                    "This user account is inactive."
                );
            });
    }

    private void account(Set<UserRole> roles, boolean active) {
        when(userDirectory.requireIdentity(ISSUER, SUBJECT))
            .thenReturn(new UserIdentity(
                USER_ID,
                "user@example.test",
                "Test User",
                roles,
                active
            ));
    }

    private void authorities(String... values) {
        doReturn(
            List.of(values)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList()
        ).when(oidcUser).getAuthorities();
    }

    private void assertRolesChanged() {
        assertThatExceptionOfType(ProblemException.class)
            .isThrownBy(() -> currentUsers.require(oidcUser))
            .satisfies(problem -> {
                assertThat(problem.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                assertThat(problem.code()).isEqualTo("roles_changed");
                assertThat(problem.getMessage()).isEqualTo(
                    "Your assigned roles changed. Sign in again to continue."
                );
            });
    }
}
