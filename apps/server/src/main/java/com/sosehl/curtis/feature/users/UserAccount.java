package com.sosehl.curtis.feature.users;

import com.sosehl.curtis.feature.users.core.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    private UUID id;

    @Column(nullable = false, length = 512)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 320)
    private String username;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "first_login_at", nullable = false)
    private Instant firstLoginAt;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    @Version
    private long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new LinkedHashSet<>();

    protected UserAccount() {}

    public static UserAccount firstLogin(
        String issuer,
        String subject,
        String username,
        String displayName,
        Set<UserRole> roles,
        Instant now
    ) {
        UserAccount account = new UserAccount();
        account.id = UUID.randomUUID();
        account.issuer = issuer;
        account.subject = subject;
        account.username = username;
        account.displayName = displayName;
        account.active = true;
        account.firstLoginAt = now;
        account.lastLoginAt = now;
        account.roles.addAll(roles);
        return account;
    }

    public void recordLogin(
        String username,
        String displayName,
        Set<UserRole> observedRoles,
        Instant now
    ) {
        this.username = username;
        this.displayName = displayName;
        this.roles.clear();
        this.roles.addAll(observedRoles);
        this.lastLoginAt = now;
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<UserRole> roles() {
        return Set.copyOf(roles);
    }

    public Instant firstLoginAt() {
        return firstLoginAt;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    public long version() {
        return version;
    }
}
