package com.nectar.workflow.dtos;

import java.time.Instant;
import java.util.UUID;

public record TaskResponseDto(
        UUID id,
        UUID tenantId,
        UUID projectId,
        UUID workflowId,
        String currentState,
        String title,
        String description,
        UUID assigneeId,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
