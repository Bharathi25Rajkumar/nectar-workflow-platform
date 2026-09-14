package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AssigneeOnlyCondition implements TransitionCondition{
    @Override
    public boolean test(Task task, User actor) {
        if(task.getAssignee() == null) return false;
        return task.getAssignee().getId().equals(actor.getId());
    }

    @Override
    public String type() {
        return "ASSIGNEE_ONLY";
    }
}
