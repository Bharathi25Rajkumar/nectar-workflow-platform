package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequestDto(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 20) String projectKey,
        @Size(max = 1000) String description
) {}
