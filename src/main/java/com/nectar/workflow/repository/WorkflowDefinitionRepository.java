package com.nectar.workflow.repository;

import com.nectar.workflow.entity.WorkflowDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    Optional<WorkflowDefinition> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<WorkflowDefinition> findByNameAndTenantId(String name, UUID tenantId);

    Page<WorkflowDefinition> findByTenantId(UUID tenantId, Pageable pageable);

}