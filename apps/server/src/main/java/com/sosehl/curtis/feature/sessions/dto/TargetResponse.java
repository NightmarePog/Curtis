package com.sosehl.curtis.feature.sessions.dto;

import java.util.UUID;

public record TargetResponse(UUID id, UUID classId, String name, String type) {}
