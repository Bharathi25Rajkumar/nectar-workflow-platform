package com.nectar.workflow.controller;

import com.nectar.workflow.dtos.CreateTaskRequestDto;
import com.nectar.workflow.dtos.TaskResponseDto;
import com.nectar.workflow.dtos.TransitionRequestDto;
import com.nectar.workflow.entity.Task;
import com.nectar.workflow.security.AuthPrincipal;
import com.nectar.workflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody CreateTaskRequestDto requestDto,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        Task task = taskService.createTask(requestDto.projectId(),
                requestDto.workflowId(), requestDto.title(), requestDto.description(), principal.username());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(task));
    }

    @GetMapping("/{id}")
    public TaskResponseDto getTask(@PathVariable UUID id) {
        return toResponseDto(taskService.getTask(id));
    }

    @GetMapping
    public Page<TaskResponseDto> list(@PageableDefault(size = 20) Pageable pageable) {
        return taskService.listTasks(pageable).map(this::toResponseDto);
    }

    @GetMapping("/project/{projectId}")
    public Page<TaskResponseDto> listByProject(@PathVariable UUID projectId,
                                               @PageableDefault(size = 20) Pageable pageable) {
        return taskService.listTasksByProject(projectId, pageable).map(this::toResponseDto);
    }

    @PostMapping("/{id}/transitions")
    public TaskResponseDto transition(@PathVariable UUID id,
                                      @Valid @RequestBody TransitionRequestDto req,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        Task task = taskService.transition(id, req.transitionName(), principal.username());
        return toResponseDto(task);
    }


    private TaskResponseDto toResponseDto(Task t) {
        return new TaskResponseDto(
                t.getId(),
                t.getTenant().getId(),
                t.getProject().getId(),
                t.getWorkflow().getId(),
                t.getCurrentState().getName(),
                t.getTitle(),
                t.getDescription(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getVersion(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

}
