package com.sosehl.curtis.feature.sessions;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SessionGroupTarget {

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "group_name", nullable = false, length = 120)
    private String name;

    protected SessionGroupTarget() {}

    public SessionGroupTarget(UUID groupId, UUID classId, String name) {
        this.groupId = Objects.requireNonNull(groupId);
        this.classId = Objects.requireNonNull(classId);
        this.name = Objects.requireNonNull(name);
    }

    public UUID getGroupId() { return groupId; }
    public UUID getClassId() { return classId; }
    public String getName() { return name; }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionGroupTarget value && groupId.equals(value.groupId);
    }

    @Override
    public int hashCode() { return groupId.hashCode(); }
}
