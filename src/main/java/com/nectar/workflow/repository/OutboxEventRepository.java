package com.nectar.workflow.repository;

import com.nectar.workflow.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
    List<OutboxEvent> findByTenantIdAndPublishedFalseOrderByCreatedAtAsc(UUID tenantId, Pageable pageable);

}
