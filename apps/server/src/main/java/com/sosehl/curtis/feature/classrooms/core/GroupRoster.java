package com.sosehl.curtis.feature.classrooms.core;

import java.util.List;
import java.util.UUID;

public record GroupRoster(
    UUID id,
    UUID classId,
    String name,
    List<UUID> activeStudentIds
) {
    public GroupRoster {
        activeStudentIds = List.copyOf(activeStudentIds);
    }
}
