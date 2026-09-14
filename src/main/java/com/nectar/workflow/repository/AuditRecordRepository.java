package com.nectar.workflow.repository;

import com.nectar.workflow.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, UUID> {

    @EntityGraph(attributePaths = {"actor"})
    Page<AuditRecord> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"actor"})
    Page<AuditRecord> findByTenantIdAndEntityId(UUID tenantId, UUID entityId, Pageable pageable);
}
