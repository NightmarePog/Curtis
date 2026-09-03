package com.sosehl.curtis.feature.sessions.attempt.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MatchAnswer(@NotNull UUID leftId, @NotNull UUID rightId) {}
