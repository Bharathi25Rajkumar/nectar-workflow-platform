package com.nectar.workflow.repository;

import com.nectar.workflow.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    @Query("SELECT t FROM WorkflowTransition t WHERE t.id = :id AND t.fromState.workflow.id = :tenantId")
    Optional<WorkflowTransition> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<WorkflowTransition> findByFromStateId(UUID fromStateId);

    @Query("SELECT t FROM WorkflowTransition t JOIN FETCH t.toState WHERE t.fromState.id = :fromStateId")
    List<WorkflowTransition> findWithToStateByFromStateId(@Param("fromStateId") UUID fromStateId);
}
