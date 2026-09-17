package com.nectar.workflow.dtos;

import java.util.UUID;

import java.util.List;

public record TransitionResponseDto(
        UUID id,
        UUID fromStateId,
        UUID toStateId,
        String name,
        String requiredRole,
        List<String> conditionTypes,
        List<String> actionTypes
) {}
