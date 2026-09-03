package com.sosehl.curtis.feature.classrooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "class_groups")
public class ClassGroupEntity {

    @Id
    private UUID id;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ClassGroupEntity() {}

    public static ClassGroupEntity create(
        UUID classId,
        String name,
        UUID creatorId,
        Instant now
    ) {
        ClassGroupEntity group = new ClassGroupEntity();
        group.id = UUID.randomUUID();
        group.classId = classId;
        group.name = name.trim();
        group.active = true;
        group.createdBy = creatorId;
        group.createdAt = now;
        group.updatedAt = now;
        return group;
    }

    public void update(String name, Boolean active, Instant now) {
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

    public UUID classId() {
        return classId;
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
}
