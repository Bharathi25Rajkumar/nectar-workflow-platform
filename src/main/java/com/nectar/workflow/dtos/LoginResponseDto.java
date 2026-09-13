package com.nectar.workflow.dtos;

import java.util.UUID;

public record LoginResponseDto (String token, String username, String role, UUID tenantId){
}
