package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.User;

public interface TransitionCondition {
    boolean test(Task task, User actor);
    String type();
}
