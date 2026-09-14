package com.nectar.workflow.dtos;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponseDto(
        UUID id,
        UUID tenantId,
        String name,
        String projectKey,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
