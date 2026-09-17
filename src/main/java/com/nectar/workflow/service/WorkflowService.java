package com.nectar.workflow.service;

import com.nectar.workflow.entity.*;
import com.nectar.workflow.exception.ResourceNotFoundException;
import com.nectar.workflow.repository.*;
import com.nectar.workflow.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final JsonMapper jsonMapper;

    public WorkflowService(WorkflowDefinitionRepository workflowDefinitionRepository,
                           WorkflowStateRepository workflowStateRepository,
                           WorkflowTransitionRepository workflowTransitionRepository,
                           TenantRepository tenantRepository,
                           UserRepository userRepository,
                           AuditService auditService,
                           OutboxService outboxService,
                           JsonMapper jsonMapper) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.jsonMapper = jsonMapper;
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

        Map<String, String> m = new LinkedHashMap<>();
        m.put("workflowId", saved.getId().toString());
        m.put("name", name);
        m.put("tenantId", tenantId.toString());
        String wfPayload;
        try { wfPayload = jsonMapper.writeValueAsString(m); } catch (Exception e) { throw new IllegalStateException("Failed to serialize payload", e); }


        outboxService.save(tenantId,"WORKFLOW",saved.getId(),"WorkflowActivated",wfPayload);
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
        WorkflowState savedState = workflowStateRepository.save(state);

        Map<String, String> m2 = new LinkedHashMap<>();
        m2.put("workflowId", workflowId.toString());
        m2.put("stateId", savedState.getId().toString());
        m2.put("name", stateName);
        String stPayload;

        try {
            stPayload = jsonMapper.writeValueAsString(m2);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }


        outboxService.save(tenantId,"WORKFLOW",workflowId,"WorkflowUpdated",stPayload);
        return savedState;
    }

    @Transactional
    public WorkflowTransition addTransition(UUID fromStateId, UUID toStateId, String name, Role requiredRole, List<String> conditionTypes, List<String> actionTypes) {

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
                .conditionTypes(conditionTypes != null ? conditionTypes : List.of("ALWAYS_TRUE"))
                .actionTypes(actionTypes != null ? actionTypes : List.of("LOG"))
                .build();

        from.addTransition(tr);

        WorkflowTransition savedTransition = workflowTransitionRepository.save(tr);

        Map<String, String> m3 = new LinkedHashMap<>();
        m3.put("workflowId", from.getWorkflow().getId().toString());
        m3.put("fromState", from.getName());
        m3.put("toState", to.getName());
        m3.put("transition", name);
        m3.put("tenantId", tenantId.toString());
        String payload;

        try {
            payload = jsonMapper.writeValueAsString(m3);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }

        outboxService.save(tenantId, "WORKFLOW", from.getWorkflow().getId(), "WorkflowUpdated", payload);
        return savedTransition;
    }

}