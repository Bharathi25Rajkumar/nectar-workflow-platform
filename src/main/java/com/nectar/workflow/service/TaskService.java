package com.nectar.workflow.service;

import com.nectar.workflow.engine.WorkflowEngine;
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
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final UserRepository userRepository;
    private final WorkflowEngine workflowEngine;
    private final AuditService auditService;
    private final OutboxService outboxService;
    private final JsonMapper jsonMapper;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       WorkflowDefinitionRepository workflowDefinitionRepository,
                       WorkflowStateRepository workflowStateRepository,
                       UserRepository userRepository,
                       WorkflowEngine workflowEngine,
                       AuditService auditService,
                       OutboxService outboxService,
                       JsonMapper jsonMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.userRepository = userRepository;
        this.workflowEngine = workflowEngine;
        this.auditService = auditService;
        this.outboxService = outboxService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public Task createTask(UUID projectId, UUID workflowId, String title, String description, String actorUsername){
        UUID tenantId = TenantContext.get();

        if (tenantId == null) throw new IllegalStateException("Tenant context missing");

        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        WorkflowDefinition workflow = workflowDefinitionRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));

        WorkflowState initial = workflowStateRepository.findByWorkflowIdAndInitialTrue(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Initial state not found for workflow"));

        User actor = userRepository.findByUsernameAndTenantId(actorUsername, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Task task = Task.builder().tenant(project.getTenant())
                .project(project)
                .workflow(workflow)
                .currentState(initial)
                .title(title)
                .description(description)
                .build();

        Task savedTask = taskRepository.save(task);

        Map<String, String> m = new LinkedHashMap<>();
        m.put("taskId", savedTask.getId().toString());
        m.put("projectId", projectId.toString());
        m.put("tenantId", tenantId.toString());
        m.put("title", title);

        String payload;
        try {
            payload = jsonMapper.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
        outboxService.save(tenantId, "TASK", savedTask.getId(), "TaskCreated", payload);
        auditService.log(tenantId, "TASK", savedTask.getId(), "CREATE", actor, "Created task: " + title);
        return savedTask;
    }

    @Transactional(readOnly = true)
    public Task getTask(UUID taskId) {
        UUID tenantId = TenantContext.get();
        return taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasks(Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return taskRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Task> listTasksByProject(UUID projectId, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return taskRepository.findByProjectIdAndTenantId(projectId, tenantId, pageable);
    }

    @Transactional
    public Task transition(UUID taskId, String transitionName, String actorUsername) {
        UUID tenantId = TenantContext.get();
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User actor = userRepository.findByUsernameAndTenantId(actorUsername, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String from = task.getCurrentState().getName();

        workflowEngine.advance(task, actor, transitionName);
        Task savedTask = taskRepository.save(task);

        Map<String, String> m2 = new LinkedHashMap<>();
        m2.put("taskId", savedTask.getId().toString());
        m2.put("fromState", from);
        m2.put("toState", savedTask.getCurrentState().getName());
        m2.put("transition", transitionName);
        m2.put("tenantId", tenantId.toString());
        String payload;
        try { payload = jsonMapper.writeValueAsString(m2); } catch (Exception e) { throw new IllegalStateException("Failed to serialize payload", e); }

        outboxService.save(tenantId, "TASK", savedTask.getId(), "TaskTransitioned", payload);

        auditService.log(tenantId, "TASK", savedTask.getId(),
                "TRANSITION:" + transitionName, actor,
                from + " -> " + savedTask.getCurrentState().getName());

        return savedTask;
    }
}
