package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.TaskHistory;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.entity.WorkflowTransition;
import com.nectar.workflow.repository.TaskHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogAction implements WorkflowAction {

    private static final Logger log = LoggerFactory.getLogger(LogAction.class);
    private final TaskHistoryRepository taskHistoryRepository;

    public LogAction(TaskHistoryRepository taskHistoryRepository){
        this.taskHistoryRepository = taskHistoryRepository;
    }


    @Override
    public void execute(Task task, User actor, WorkflowTransition transition) {

        TaskHistory history = TaskHistory.builder()
                .tenantId(task.getTenant().getId())
                .task(task)
                .fromState(transition.getFromState())
                .toState(transition.getToState())
                .transitionName(transition.getName())
                .actor(actor)
                .build();
        taskHistoryRepository.save(history);

        log.info("Task {} transition '{}' by user {}: {} -> {}",
                task.getId(),
                transition.getName(),
                actor.getUsername(),
                transition.getFromState().getName(),
                transition.getToState().getName()
        );
    }

    @Override
    public String type() {
        return "LOG";
    }
}
