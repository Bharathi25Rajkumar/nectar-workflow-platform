package com.nectar.workflow.service;

import com.nectar.workflow.entity.OutboxEvent;
import com.nectar.workflow.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEvent save(UUID tenantId, String aggregateType, UUID aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .build();
        return outboxEventRepository.save(event);
    }
}
