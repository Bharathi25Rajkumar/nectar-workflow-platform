package com.nectar.workflow.controller;

import com.nectar.workflow.dtos.*;
import com.nectar.workflow.entity.Role;
import com.nectar.workflow.entity.WorkflowDefinition;
import com.nectar.workflow.entity.WorkflowState;
import com.nectar.workflow.entity.WorkflowTransition;
import com.nectar.workflow.security.AuthPrincipal;
import com.nectar.workflow.service.WorkflowService;
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
@RequestMapping("/api/workflows")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponseDto> createWorkflow(@Valid @RequestBody CreateWorkflowRequestDto requestDto,
                                                      @AuthenticationPrincipal AuthPrincipal principal) {
        WorkflowDefinition wd = workflowService.createWorkflow(requestDto.name(), requestDto.description(), principal.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(wd));
    }

    @GetMapping("/{id}")
    public WorkflowResponseDto get(@PathVariable UUID id) {
        return toResponseDto(workflowService.getWorkflow(id));
    }

    @GetMapping
    public Page<WorkflowResponseDto> listWorkflows(@PageableDefault(size = 20) Pageable pageable) {
        return workflowService.listWorkflows(pageable).map(this::toResponseDto);
    }

    @PostMapping("/{workflowId}/states")
    public ResponseEntity<StateResponseDto> addState(@PathVariable UUID workflowId,
                                                     @Valid @RequestBody CreateStateRequestDto requestDto) {
        WorkflowState s = workflowService.addState(workflowId, requestDto.name(), requestDto.initial(), requestDto.terminal());
        return ResponseEntity.status(HttpStatus.CREATED).body(new StateResponseDto(s.getId(), s.getWorkflow().getId(), s.getName(), s.isInitial(), s.isTerminal()));
    }

    @PostMapping("/states/{fromStateId}/transitions")
    public ResponseEntity<TransitionResponseDto> addTransition(@PathVariable UUID fromStateId,
                                                               @Valid @RequestBody CreateTransitionRequestDto requestDto) {
        Role role = requestDto.requiredRole() != null ? Role.valueOf(requestDto.requiredRole()) : null;
        WorkflowTransition tr = workflowService.addTransition(fromStateId, UUID.fromString(requestDto.toStateId()), requestDto.name(),
                role, requestDto.conditionType(), requestDto.actionType());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransitionResponseDto(tr.getId(), tr.getFromState().getId(),
                tr.getToState().getId(), tr.getName(), tr.getRequiredRole() != null ? tr.getRequiredRole().name() : null, tr.getConditionType(), tr.getActionType()));
    }

    private WorkflowResponseDto toResponseDto(WorkflowDefinition wd) {
        return new WorkflowResponseDto(
                wd.getId(),
                wd.getTenant().getId(),
                wd.getName(),
                wd.getDescription(),
                wd.getVersion(),
                wd.getCreatedAt(),
                wd.getUpdatedAt()
        );
    }

}