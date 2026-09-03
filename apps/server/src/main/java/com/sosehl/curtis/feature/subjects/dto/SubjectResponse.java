package com.sosehl.curtis.feature.subjects.dto;

import com.sosehl.curtis.feature.users.core.UserSummary;
import java.util.List;
import java.util.UUID;

public record SubjectResponse(
    UUID id,
    String code,
    String name,
    boolean active,
    long version,
    List<UserSummary> teachers
) {}
