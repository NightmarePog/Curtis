package com.sosehl.curtis.feature.classrooms.core;

import java.util.Set;
import java.util.UUID;
public record AudienceStudent(
    UUID studentId,
    UUID classId,
    Set<UUID> targetedGroupIds
) {
    public AudienceStudent {
        targetedGroupIds = Set.copyOf(targetedGroupIds);
    }
}
