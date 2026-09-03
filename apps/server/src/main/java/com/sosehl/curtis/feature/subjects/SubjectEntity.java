package com.sosehl.curtis.feature.subjects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class SubjectEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected SubjectEntity() {}

    public static SubjectEntity create(
        String code,
        String name,
        Instant now
    ) {
        SubjectEntity subject = new SubjectEntity();
        subject.id = UUID.randomUUID();
        subject.code = normalizeCode(code);
        subject.name = name.trim();
        subject.active = true;
        subject.createdAt = now;
        subject.updatedAt = now;
        return subject;
    }

    public void update(
        String code,
        String name,
        Boolean active,
        Instant now
    ) {
        if (code != null) {
            this.code = normalizeCode(code);
        }
        if (name != null) {
            this.name = name.trim();
        }
        if (active != null) {
            this.active = active;
        }
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public boolean active() {
        return active;
    }

    public long version() {
        return version;
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
