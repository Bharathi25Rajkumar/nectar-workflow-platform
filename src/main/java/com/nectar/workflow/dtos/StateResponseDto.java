package com.nectar.workflow.dtos;

import java.util.UUID;

public record StateResponseDto(
        UUID id,
        UUID workflowId,
        String name,
        boolean initial,
        boolean terminal
) {}
