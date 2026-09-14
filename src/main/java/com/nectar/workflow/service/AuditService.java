package com.nectar.workflow.service;

import com.nectar.workflow.entity.AuditRecord;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.repository.AuditRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditRecordRepository auditRecordRepository;

    public AuditService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @Transactional
    public AuditRecord log(UUID tenantId, String entityType, UUID entityId, String action, User actor, String details) {
        AuditRecord record = AuditRecord.builder()
                .tenantId(tenantId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .actor(actor)
                .details(details)
                .build();
        return auditRecordRepository.save(record);
    }
}
