package com.sosehl.curtis.feature.users;

import com.sosehl.curtis.feature.users.core.UserAccessRevoked;
import com.sosehl.curtis.feature.users.core.UserDirectory;
import com.sosehl.curtis.feature.users.core.UserIdentity;
import com.sosehl.curtis.feature.users.core.UserRole;
import com.sosehl.curtis.feature.users.core.UserSummary;
import com.sosehl.curtis.platform.persistence.SOSE_ReadOnlyTransaction;
import com.sosehl.curtis.shared.errors.ProblemException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@SOSE_ReadOnlyTransaction
public class UserService implements UserDirectory {

    private final UserAccountRepository repository;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public UserService(
        UserAccountRepository repository,
        Clock clock,
        ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    @Override
    public UserSummary recordLogin(
        String issuer,
        String subject,
        String username,
        String displayName,
        Set<UserRole> roles
    ) {
        String normalizedIssuer = requireText(issuer, "issuer");
        String normalizedSubject = requireText(subject, "subject");
        String normalizedUsername = normalizeUsername(username);
        String normalizedDisplayName = hasText(displayName)
            ? displayName.trim()
            : normalizedUsername;
        Instant now = clock.instant();

        UserAccount account = repository
            .findByIssuerAndSubject(normalizedIssuer, normalizedSubject)
            .orElseGet(() ->
                UserAccount.firstLogin(
                    normalizedIssuer,
                    normalizedSubject,
                    normalizedUsername,
                    normalizedDisplayName,
                    roles,
                    now
                )
            );

        if (account.version() != 0 || repository.existsById(account.id())) {
            account.recordLogin(
                normalizedUsername,
                normalizedDisplayName,
                roles,
                now
            );
        }

        try {
            return toSummary(repository.saveAndFlush(account));
        } catch (DataIntegrityViolationException exception) {
            throw ProblemException.conflict(
                "identity_username_conflict",
                "The verified username is already linked to another identity."
            );
        }
    }

    @Override
    public UserIdentity requireIdentity(String issuer, String subject) {
        UserAccount account = requireAccountByIdentity(issuer, subject);
        return new UserIdentity(
            account.id(),
            account.username(),
            account.displayName(),
            account.roles(),
            account.active()
        );
    }

    @Override
    public UserSummary require(UUID id) {
        return toSummary(requireAccount(id));
    }

    @Override
    public List<UserSummary> summariesByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, UserSummary> summaries = new LinkedHashMap<>();
        repository
            .findAllByIdIn(ids)
            .forEach(account -> summaries.put(account.id(), toSummary(account)));
        return ids
            .stream()
            .distinct()
            .map(summaries::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Override
    public Set<UUID> activeIdsWithRole(UserRole role) {
        return repository
            .findDistinctByActiveTrueAndRolesContaining(role)
            .stream()
            .map(UserAccount::id)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public UserSummary requireActiveTeacher(UUID id) {
        UserAccount account = requireAccount(id);
        if (!account.active() || !account.roles().contains(UserRole.TEACHER)) {
            throw ProblemException.notFound(
                "teacher_not_found",
                "No active verified teacher has that id."
            );
        }
        return toSummary(account);
    }

    @Override
    public UserSummary requireActiveTeacherByUsername(String username) {
        UserAccount account = repository
            .findByUsernameIgnoreCase(normalizeUsername(username))
            .filter(UserAccount::active)
            .filter(candidate -> candidate.roles().contains(UserRole.TEACHER))
            .orElseThrow(() ->
                ProblemException.notFound(
                    "teacher_not_found",
                    "No active verified teacher has that username."
                )
            );
        return toSummary(account);
    }

    public List<UserAccountSnapshot> list(
        UserRole role,
        Boolean active
    ) {
        return repository
            .findAllByOrderByDisplayNameAsc()
            .stream()
            .filter(account -> role == null || account.roles().contains(role))
            .filter(account -> active == null || account.active() == active)
            .map(this::snapshot)
            .toList();
    }

    @Transactional
    public UserAccountSnapshot setActive(
        UUID actorId,
        UUID userId,
        boolean active,
        long expectedVersion
    ) {
        UserAccount account = requireAccount(userId);
        if (actorId.equals(userId) && !active) {
            throw ProblemException.badRequest(
                "cannot_deactivate_self",
                "Administrators cannot deactivate their own account."
            );
        }
        if (account.version() != expectedVersion) {
            throw ProblemException.conflict(
                "user_version_conflict",
                "The user changed since it was loaded."
            );
        }
        account.setActive(active);
        UserAccountSnapshot result = snapshot(repository.saveAndFlush(account));
        if (!active) {
            events.publishEvent(new UserAccessRevoked(userId));
        }
        return result;
    }

    @Override
    public void requireRole(UUID userId, UserRole role) {
        UserAccount account = requireAccount(userId);
        if (!account.active() || !account.roles().contains(role)) {
            throw ProblemException.notFound(
                "eligible_user_not_found",
                "No active verified user with the required role was found."
            );
        }
    }

    private UserAccount requireAccountByIdentity(
        String issuer,
        String subject
    ) {
        return repository
            .findByIssuerAndSubject(issuer, subject)
            .orElseThrow(() ->
                ProblemException.unauthorized(
                    "identity_not_registered",
                    "The signed-in identity has not been registered."
                )
            );
    }

    private UserAccount requireAccount(UUID id) {
        return repository
            .findById(id)
            .orElseThrow(() ->
                ProblemException.notFound(
                    "user_not_found",
                    "The user does not exist."
                )
            );
    }

    private UserSummary toSummary(UserAccount account) {
        return new UserSummary(
            account.id(),
            account.displayName(),
            account.username(),
            account.roles()
        );
    }

    private UserAccountSnapshot snapshot(UserAccount account) {
        return new UserAccountSnapshot(
            account.id(),
            account.username(),
            account.displayName(),
            account.roles(),
            account.active(),
            account.firstLoginAt(),
            account.lastLoginAt(),
            account.version()
        );
    }

    private String normalizeUsername(String username) {
        return requireText(username, "username").toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String field) {
        if (!hasText(value)) {
            throw ProblemException.badRequest(
                "invalid_identity_claim",
                "The identity claim '" + field + "' is missing."
            );
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
