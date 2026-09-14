package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTransitionRequestDto(
        @NotBlank String toStateId,
        @NotBlank @Size(max = 100) String name,
        String requiredRole,
        String conditionType,
        String actionType
) {}
