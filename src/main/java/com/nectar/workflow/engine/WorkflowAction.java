package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.User;
import com.nectar.workflow.entity.WorkflowTransition;

public interface WorkflowAction {

    void execute(Task task, User actor, WorkflowTransition transition);

    String type();
}
