package com.nectar.workflow.messaging.kafka;

import java.time.Instant;
import java.util.UUID;

public record WorkflowEvent(
        UUID id,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        UUID tenantId,
        String payload,
        Instant createdAt
) {}
