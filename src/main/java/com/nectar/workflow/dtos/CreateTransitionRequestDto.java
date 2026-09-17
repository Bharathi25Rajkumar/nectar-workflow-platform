package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTransitionRequestDto(
        @NotBlank String toStateId,
        @NotBlank String name,
        String requiredRole,
        List<String> conditionTypes,
        List<String> actionTypes
) {}
