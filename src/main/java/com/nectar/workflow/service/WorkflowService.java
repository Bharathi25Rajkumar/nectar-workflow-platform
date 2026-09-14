package com.nectar.workflow.service;

import com.nectar.workflow.entity.*;
import com.nectar.workflow.exception.ResourceNotFoundException;
import com.nectar.workflow.repository.*;
import com.nectar.workflow.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public WorkflowService(WorkflowDefinitionRepository workflowDefinitionRepository,
                           WorkflowStateRepository workflowStateRepository,
                           WorkflowTransitionRepository workflowTransitionRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository,
                           AuditService auditService) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public WorkflowDefinition createWorkflow(String name, String description, String actorUsername) {
        UUID tenantId = TenantContext.get();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        User actor = userRepository.findByUsernameAndTenantId(actorUsername, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        WorkflowDefinition wd = WorkflowDefinition.builder()
                .tenant(tenant)
                .name(name)
                .description(description)
                .build();
        WorkflowDefinition saved = workflowDefinitionRepository.save(wd);
        auditService.log(tenantId, "WORKFLOW", saved.getId(), "CREATE", actor, "Created workflow " + name);
        return saved;
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition getWorkflow(UUID id) {
        UUID tenantId = TenantContext.get();
        return workflowDefinitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowDefinition> listWorkflows(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return workflowDefinitionRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional
    public WorkflowState addState(UUID workflowId, String stateName, boolean initial, boolean terminal) {
        UUID tenantId = TenantContext.get();
        WorkflowDefinition wd = workflowDefinitionRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
        WorkflowState state = WorkflowState.builder()
                .workflow(wd)
                .name(stateName)
                .initial(initial)
                .terminal(terminal)
                .build();
        return workflowStateRepository.save(state);
    }

    @Transactional
    public WorkflowTransition addTransition(UUID fromStateId, UUID toStateId, String name, Role requiredRole, String conditionType, String actionType) {
        WorkflowState from = workflowStateRepository.findById(fromStateId)
                .orElseThrow(() -> new ResourceNotFoundException("From state not found"));
        WorkflowState to = workflowStateRepository.findById(toStateId)
                .orElseThrow(() -> new ResourceNotFoundException("To state not found"));
        // tenant check via fromState.workflow.tenant ensures same tenant
        UUID tenantId = TenantContext.get();
        if (!from.getWorkflow().getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("State not found");
        }
        WorkflowTransition tr = WorkflowTransition.builder()
                .fromState(from)
                .toState(to)
                .name(name)
                .requiredRole(requiredRole)
                .conditionType(conditionType != null ? conditionType : "ALWAYS_TRUE")
                .actionType(actionType != null ? actionType : "LOG")
                .build();
        from.addTransition(tr);
        return workflowTransitionRepository.save(tr);
    }

}