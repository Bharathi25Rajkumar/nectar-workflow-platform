package com.nectar.workflow.dtos;

import java.util.UUID;

public record TransitionResponseDto(
        UUID id,
        UUID fromStateId,
        UUID toStateId,
        String name,
        String requiredRole,
        String conditionType,
        String actionType
) {}
