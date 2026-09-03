package com.sosehl.curtis.feature.classrooms.dto;

import com.sosehl.curtis.feature.users.core.UserSummary;
import java.util.List;
import java.util.UUID;

public record ClassroomResponse(
    UUID id,
    String name,
    boolean active,
    long version,
    List<UserSummary> teachers,
    List<UserSummary> students,
    List<GroupResponse> groups
) {
    public ClassroomResponse {
        teachers = List.copyOf(teachers);
        students = List.copyOf(students);
        groups = List.copyOf(groups);
    }

    public record GroupResponse(
        UUID id,
        String name,
        boolean active,
        long version,
        List<UserSummary> students
    ) {
        public GroupResponse {
            students = List.copyOf(students);
        }
    }
}
