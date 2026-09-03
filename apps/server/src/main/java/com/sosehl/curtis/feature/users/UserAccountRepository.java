package com.sosehl.curtis.feature.users;

import com.sosehl.curtis.feature.users.core.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository
    extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByIssuerAndSubject(String issuer, String subject);

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    List<UserAccount> findAllByIdIn(Collection<UUID> ids);

    List<UserAccount> findAllByOrderByDisplayNameAsc();

    List<UserAccount> findDistinctByActiveTrueAndRolesContaining(UserRole role);
}
