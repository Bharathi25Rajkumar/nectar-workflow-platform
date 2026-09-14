package com.nectar.workflow.repository;

import com.nectar.workflow.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndTenantId(UUID uuid, UUID tenantId);
    Optional<Project> findByProjectKeyAndTenantId(String projectKey, UUID tenantId);
    Page<Project> findByTenantId(UUID tenantId, Pageable pageable);

}
