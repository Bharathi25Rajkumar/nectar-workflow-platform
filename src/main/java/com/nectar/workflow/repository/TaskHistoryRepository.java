package com.nectar.workflow.repository;

import com.nectar.workflow.entity.TaskHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {

    @EntityGraph(attributePaths = {"fromState", "toState", "actor"})
    Page<TaskHistory> findByTenantIdAndTaskId(UUID tenantId, UUID taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"fromState", "toState"})
    Page<TaskHistory> findByTenantId(UUID tenantId, Pageable pageable);
}
