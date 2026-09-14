package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStateRequestDto(
        @NotBlank @Size(max = 100) String name,
        boolean initial,
        boolean terminal
) {}
