package com.sosehl.curtis.feature.subjects.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class SubjectRequests {

    private SubjectRequests() {}

    public record CreateSubjectRequest(
        @NotBlank @Size(max = 32) String code,
        @NotBlank @Size(max = 100) String name
    ) {}

    public record UpdateSubjectRequest(
        @Size(min = 1, max = 32)
        @Pattern(
            regexp = "(?s).*\\S.*",
            message = "must contain a non-whitespace character"
        )
        String code,
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
