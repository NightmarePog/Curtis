package com.sosehl.curtis.feature.classrooms.core;

import java.util.Set;
import java.util.UUID;

public interface ClassroomAudienceResolver {

    ResolvedAudience resolveAudience(
        UUID teacherId,
        Set<UUID> classIds,
        Set<UUID> groupIds
    );
}
