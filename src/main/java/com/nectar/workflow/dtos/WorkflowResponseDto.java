package com.nectar.workflow.dtos;

import java.time.Instant;
import java.util.UUID;

public record WorkflowResponseDto(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
