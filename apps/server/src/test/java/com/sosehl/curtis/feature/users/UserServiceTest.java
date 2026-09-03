package com.sosehl.curtis.feature.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.sosehl.curtis.feature.users.core.UserAccessRevoked;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-02T10:00:00Z");

    @Mock
    private UserAccountRepository repository;

    @Mock
    private ApplicationEventPublisher events;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            events
        );
        lenient().when(repository.saveAndFlush(any(UserAccount.class))).thenAnswer(
            invocation -> invocation.getArgument(0)
        );
    }

    @Test
    void firstVerifiedLoginStoresEveryObservedRole() {
        when(repository.findByIssuerAndSubject("issuer", "subject"))
            .thenReturn(Optional.empty());

        UserSummary result = service.recordLogin(
            "issuer",
            "subject",
            "Teacher@School.cz",
            "Ada Teacher",
            Set.of(UserRole.TEACHER, UserRole.ADMINISTRATOR)
        );

        assertThat(result.username()).isEqualTo("teacher@school.cz");
        assertThat(result.displayName()).isEqualTo("Ada Teacher");
        assertThat(result.roles()).containsExactlyInAnyOrder(
            UserRole.TEACHER,
            UserRole.ADMINISTRATOR
        );
    }

    @Test
    void laterLoginSynchronizesProfileAndRoles() {
        UserAccount account = UserAccount.firstLogin(
            "issuer",
            "subject",
            "old@school.cz",
            "Old Name",
            Set.of(UserRole.TEACHER),
            NOW.minusSeconds(60)
        );
        when(repository.findByIssuerAndSubject("issuer", "subject"))
            .thenReturn(Optional.of(account));
        when(repository.existsById(account.id())).thenReturn(true);

        UserSummary result = service.recordLogin(
            "issuer",
            "subject",
            "new@school.cz",
            "New Name",
            Set.of(UserRole.STUDENT)
        );

        assertThat(result.username()).isEqualTo("new@school.cz");
        assertThat(result.displayName()).isEqualTo("New Name");
        assertThat(result.roles()).containsExactly(UserRole.STUDENT);
    }

    @Test
    void inactiveUserCannotBeResolvedAsTeacher() {
        UserAccount account = UserAccount.firstLogin(
            "issuer",
            "subject",
            "teacher@school.cz",
            "Teacher",
            Set.of(UserRole.TEACHER),
            NOW
        );
        account.setActive(false);
        when(repository.findById(account.id())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.requireActiveTeacher(account.id()))
            .isInstanceOf(ProblemException.class)
            .satisfies(exception ->
                assertThat(((ProblemException) exception).code())
                    .isEqualTo("teacher_not_found")
            );
    }

    @Test
    void deactivatingUserPublishesImmediateAccessRevocation() {
        UserAccount account = UserAccount.firstLogin(
            "issuer",
            "subject",
            "student@school.cz",
            "Student",
            Set.of(UserRole.STUDENT),
            NOW
        );
        when(repository.findById(account.id())).thenReturn(Optional.of(account));

        service.setActive(
            java.util.UUID.randomUUID(),
            account.id(),
            false,
            account.version()
        );

        verify(events).publishEvent(new UserAccessRevoked(account.id()));
    }
}
