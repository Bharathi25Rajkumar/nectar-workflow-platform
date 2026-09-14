package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;

public record TransitionRequestDto(
        @NotBlank String transitionName
) {}
