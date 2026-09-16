package com.nectar.workflow.repository;

import com.nectar.workflow.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    @EntityGraph(attributePaths = {"currentState", "project", "assignee", "tenant"})
    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"currentState", "project", "tenant"})
    Page<Task> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"currentState", "project", "tenant"})
    Page<Task> findByProjectIdAndTenantId(UUID projectId, UUID tenantId, Pageable pageable);

    Page<Task> findByCurrentStateId(UUID stateId, Pageable pageable);
}
