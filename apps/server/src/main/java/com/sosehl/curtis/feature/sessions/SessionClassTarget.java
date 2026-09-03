package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionClassTarget {

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "class_name", nullable = false, length = 120)
    private String name;

    protected SessionClassTarget() {}

    public SessionClassTarget(UUID classId, String name) {
        this.classId = Objects.requireNonNull(classId);
        this.name = Objects.requireNonNull(name);
    }

    public UUID getClassId() { return classId; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionClassTarget value && classId.equals(value.classId);
    }

    @Override
    public int hashCode() { return classId.hashCode(); }
}
