package com.nectar.workflow.dtos;

import java.time.Instant;

public record ErrorResponseDto(Instant timestamp, int status, String error, String message) {
}
