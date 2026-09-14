package com.nectar.workflow.engine;

import com.nectar.workflow.entity.Task;
import com.nectar.workflow.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AlwaysTrueCondition implements TransitionCondition {
    @Override
    public boolean test(Task task, User actor) {
        return true;
    }

    @Override
    public String type() {
        return "ALWAYS_TRUE";
    }
}
