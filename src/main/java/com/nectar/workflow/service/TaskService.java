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

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       WorkflowDefinitionRepository workflowDefinitionRepository,
                       WorkflowStateRepository workflowStateRepository,
                       UserRepository userRepository,
                       WorkflowEngine workflowEngine,
                       AuditService auditService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.userRepository = userRepository;
        this.workflowEngine = workflowEngine;
        this.auditService = auditService;
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
        auditService.log(tenantId, "TASK", savedTask.getId(),
                "TRANSITION:" + transitionName, actor,
                from + " -> " + savedTask.getCurrentState().getName());

        return savedTask;
    }
}
