package com.sosehl.curtis.feature.subjects.core;

import java.util.UUID;
public record SubjectSummary(
    UUID id,
    String code,
    String name,
    boolean active,
    long version
) {}
