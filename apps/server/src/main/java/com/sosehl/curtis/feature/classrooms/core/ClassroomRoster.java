package com.sosehl.curtis.feature.classrooms.core;

import java.util.List;
import java.util.UUID;

public record ClassroomRoster(
    UUID id,
    String name,
    boolean active,
    List<UUID> activeStudentIds,
    List<GroupRoster> activeGroups
) {
    public ClassroomRoster {
        activeStudentIds = List.copyOf(activeStudentIds);
        activeGroups = List.copyOf(activeGroups);
    }
}
