package com.sosehl.curtis.feature.classrooms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class ClassroomRequests {

    private ClassroomRequests() {}

    public record CreateClass(
        @NotBlank @Size(max = 100) String name
    ) {}

    public record UpdateClass(
        @Size(min = 1, max = 100)
        @Pattern(
            regexp = "(?s).*\\S.*",
            message = "must contain a non-whitespace character"
        )
        String name,
        Boolean active,
        @PositiveOrZero long version
    ) {}

    public record CreateGroup(
        @NotBlank @Size(max = 100) String name
    ) {}

    public record UpdateGroup(
        @Size(min = 1, max = 100)
        @Pattern(
            regexp = "(?s).*\\S.*",
            message = "must contain a non-whitespace character"
        )
        String name,
        Boolean active,
        @PositiveOrZero long version
    ) {}
}
