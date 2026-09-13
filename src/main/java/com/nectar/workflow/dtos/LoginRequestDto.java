package com.nectar.workflow.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank String username,
                              @NotBlank String password,
                              @NotBlank String tenantSlug) {
}
