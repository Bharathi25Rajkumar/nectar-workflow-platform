package com.nectar.workflow.repository;

import com.nectar.workflow.entity.WorkflowState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, UUID>{

    @Query("SELECT s FROM WorkflowState s WHERE s.id = :id AND s.workflow.tenant.id = :tenantId")
    Optional<WorkflowState> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    Page<WorkflowState> findByWorkflowId(UUID workflowId, Pageable pageable);

    Optional<WorkflowState> findByWorkflowIdAndName(UUID workflowId, String name);

    Optional<WorkflowState> findByWorkflowIdAndInitialTrue(UUID workflowId);
}