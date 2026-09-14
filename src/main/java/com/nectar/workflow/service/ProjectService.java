package com.nectar.workflow.service;

import com.nectar.workflow.entity.Project;
import com.nectar.workflow.entity.Tenant;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.exception.ResourceNotFoundException;
import com.nectar.workflow.repository.ProjectRepository;
import com.nectar.workflow.repository.TenantRepository;
import com.nectar.workflow.repository.UserRepository;
import com.nectar.workflow.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository,
                          TenantRepository tenantRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Project createProject(String name, String projectKey, String description, String actorUsername) {
        UUID tenantId = TenantContext.get();
        if (tenantId == null) throw new IllegalStateException("Tenant context missing");
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        User actor = userRepository.findByUsernameAndTenantId(actorUsername, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Project project = Project.builder()
                .tenant(tenant)
                .name(name)
                .projectKey(projectKey)
                .description(description)
                .build();
        Project saved = projectRepository.save(project);
        auditService.log(tenantId, "PROJECT", saved.getId(), "CREATE", actor, "Created project " + name);
        return saved;
    }

    @Transactional(readOnly = true)
    public Project getProject(UUID id) {
        UUID tenantId = TenantContext.get();
        return projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    @Transactional(readOnly = true)
    public Page<Project> listProjects(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return projectRepository.findByTenantId(tenantId, pageable);
    }
}
