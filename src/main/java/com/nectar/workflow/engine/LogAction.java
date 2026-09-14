package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.entity.WorkflowTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogAction implements WorkflowAction {

    private static final Logger log = LoggerFactory.getLogger(LogAction.class);
    @Override
    public void execute(Task task, User actor, WorkflowTransition transition) {
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
