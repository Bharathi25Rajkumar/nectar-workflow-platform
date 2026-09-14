package com.nectar.workflow.controller;

import com.nectar.workflow.dtos.CreateProjectRequestDto;
import com.nectar.workflow.dtos.ProjectResponseDto;
import com.nectar.workflow.entity.Project;
import com.nectar.workflow.security.AuthPrincipal;
import com.nectar.workflow.service.ProjectService;
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
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(@Valid @RequestBody CreateProjectRequestDto requestDto,
                                                     @AuthenticationPrincipal AuthPrincipal principal) {
        Project p = projectService.createProject(requestDto.name(), requestDto.projectKey(), requestDto.description(), principal.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(p));
    }

    @GetMapping("/{id}")
    public ProjectResponseDto get(@PathVariable UUID id) {
        return toResponseDto(projectService.getProject(id));
    }

    @GetMapping
    public Page<ProjectResponseDto> listProjects(@PageableDefault(size = 20) Pageable pageable) {
        return projectService.listProjects(pageable).map(this::toResponseDto);
    }

    private ProjectResponseDto toResponseDto(Project p) {
        return new ProjectResponseDto(
                p.getId(),
                p.getTenant().getId(),
                p.getName(),
                p.getProjectKey(),
                p.getDescription(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

}