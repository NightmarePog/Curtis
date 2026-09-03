package com.sosehl.curtis.feature.classrooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "classes")
public class ClassroomEntity {

    @Id
    private UUID id;

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

    protected ClassroomEntity() {}

    public static ClassroomEntity create(
        String name,
        UUID creatorId,
        Instant now
    ) {
        ClassroomEntity classroom = new ClassroomEntity();
        classroom.id = UUID.randomUUID();
        classroom.name = name.trim();
        classroom.active = true;
        classroom.createdBy = creatorId;
        classroom.createdAt = now;
        classroom.updatedAt = now;
        return classroom;
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
