package com.nectar.workflow.security;

import java.util.UUID;

public record AuthPrincipal(UUID tenantId, UUID userId, String username, String role) {
}
