package com.sosehl.curtis.feature.classrooms.core;

import java.util.List;
public record ResolvedAudience(
    List<ClassSummary> classes,
    List<GroupSummary> groups,
    List<AudienceStudent> students
) {
    public ResolvedAudience {
        classes = List.copyOf(classes);
        groups = List.copyOf(groups);
        students = List.copyOf(students);
    }
}
