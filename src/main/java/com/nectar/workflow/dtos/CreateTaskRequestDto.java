package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequestDto(
        @NotNull UUID projectId,
        @NotNull UUID workflowId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description
) {}
