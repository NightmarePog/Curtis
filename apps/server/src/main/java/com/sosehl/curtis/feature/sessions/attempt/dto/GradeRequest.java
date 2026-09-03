package com.sosehl.curtis.feature.sessions.attempt.dto;

import jakarta.validation.constraints.Min;

public record GradeRequest(@Min(0) int points) {}
