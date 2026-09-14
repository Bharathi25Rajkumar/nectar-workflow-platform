package com.nectar.workflow.engine;

import com.nectar.workflow.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WorkflowEngineTest {

    private WorkflowEngine engine;
    private Tenant tenant;
    private WorkflowDefinition workflow;
    private WorkflowState todo;
    private WorkflowState inProgress;
    private WorkflowState done;
    private User manager;
    private User employee;
    private User admin;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(UUID.randomUUID()).slug("acme").name("Acme").build();

        workflow = WorkflowDefinition.builder().id(UUID.randomUUID()).tenant(tenant).name("Test WF").build();

        todo = WorkflowState.builder().id(UUID.randomUUID()).workflow(workflow).name("TODO").initial(true).terminal(false).build();
        inProgress = WorkflowState.builder().id(UUID.randomUUID()).workflow(workflow).name("IN_PROGRESS").initial(false).terminal(false).build();
        done = WorkflowState.builder().id(UUID.randomUUID()).workflow(workflow).name("DONE").initial(false).terminal(true).build();

        manager = User.builder().id(UUID.randomUUID()).tenant(tenant).username("manager").role(Role.MANAGER).build();
        employee = User.builder().id(UUID.randomUUID()).tenant(tenant).username("employee").role(Role.EMPLOYEE).build();
        admin = User.builder().id(UUID.randomUUID()).tenant(tenant).username("admin").role(Role.ADMIN).build();

        engine = new WorkflowEngine(
                java.util.List.of(new AlwaysTrueCondition(), new AssigneeOnlyCondition()),
                java.util.List.of(new LogAction(null))
        );
    }

    @Test
    void advance_validTransition_succeeds() {
        WorkflowTransition t = WorkflowTransition.builder()
                .id(UUID.randomUUID()).fromState(todo).toState(inProgress)
                .name("start").requiredRole(Role.MANAGER).conditionType("ALWAYS_TRUE").actionType("LOG").build();
        todo.addTransition(t);

        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(todo).title("T").build();

        Task result = engine.advance(task, manager, "start");
        assertEquals("IN_PROGRESS", result.getCurrentState().getName());
    }

    @Test
    void advance_invalidTransition_throws404() {
        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(todo).title("T").build();

        assertThrows(com.nectar.workflow.exception.ResourceNotFoundException.class,
                () -> engine.advance(task, manager, "unknown"));
    }

    @Test
    void advance_roleDenied_throws403() {
        WorkflowTransition t = WorkflowTransition.builder()
                .id(UUID.randomUUID()).fromState(todo).toState(inProgress)
                .name("start").requiredRole(Role.MANAGER).conditionType("ALWAYS_TRUE").actionType("LOG").build();
        todo.addTransition(t);
        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(todo).title("T").build();

        assertThrows(AccessDeniedException.class,
                () -> engine.advance(task, employee, "start"));
    }

    @Test
    void advance_adminCanDoManagerTransition() {
        WorkflowTransition t = WorkflowTransition.builder()
                .id(UUID.randomUUID()).fromState(todo).toState(inProgress)
                .name("start").requiredRole(Role.MANAGER).conditionType("ALWAYS_TRUE").actionType("LOG").build();
        todo.addTransition(t);
        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(todo).title("T").build();

        Task result = engine.advance(task, admin, "start");
        assertEquals("IN_PROGRESS", result.getCurrentState().getName());
    }

    @Test
    void advance_assigneeOnlyFails_whenNotAssignee() {
        WorkflowTransition t = WorkflowTransition.builder()
                .id(UUID.randomUUID()).fromState(inProgress).toState(done)
                .name("complete").requiredRole(Role.MANAGER).conditionType("ASSIGNEE_ONLY").actionType("LOG").build();
        inProgress.addTransition(t);
        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(inProgress).title("T").assignee(employee).build();

        assertThrows(IllegalStateException.class,
                () -> engine.advance(task, manager, "complete"));
    }

    @Test
    void advance_assigneeOnlySucceeds_whenAssignee() {
        WorkflowTransition t = WorkflowTransition.builder()
                .id(UUID.randomUUID()).fromState(inProgress).toState(done)
                .name("complete").requiredRole(Role.EMPLOYEE).conditionType("ASSIGNEE_ONLY").actionType("LOG").build();
        inProgress.addTransition(t);
        Task task = Task.builder().id(UUID.randomUUID()).tenant(tenant)
                .project(Project.builder().id(UUID.randomUUID()).tenant(tenant).name("P").projectKey("P1").build())
                .workflow(workflow).currentState(inProgress).title("T").assignee(employee).build();

        Task result = engine.advance(task, employee, "complete");
        assertEquals("DONE", result.getCurrentState().getName());
    }
}
