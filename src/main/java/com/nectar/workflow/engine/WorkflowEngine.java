package com.nectar.workflow.engine;

import com.nectar.workflow.entity.*;
import com.nectar.workflow.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WorkflowEngine {

    private final Map<String, TransitionCondition> conditions;
    private final Map<String, WorkflowAction> actions;

    public WorkflowEngine(List<TransitionCondition> conditionList, List<WorkflowAction> actionList){
        this.conditions = conditionList.stream().collect(Collectors.toMap(TransitionCondition::type, Function.identity()));
        this.actions = actionList.stream().collect((Collectors.toMap(WorkflowAction::type, Function.identity())));
    }

    @Transactional
    public Task advance(Task task, User actor, String transitionName){
        WorkflowState currentState = task.getCurrentState();

        WorkflowTransition workflowTransition = currentState.getTransitions()
                .stream().filter(t -> t.getName().equals(transitionName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transition " + transitionName + " not found from state " + currentState.getName()));

        if(workflowTransition.getRequiredRole() != null){
            Role requiredRole = workflowTransition.getRequiredRole();
            Role actualRole = actor.getRole();
            boolean allowed = actualRole == requiredRole || actualRole == Role.ADMIN;
            if(!allowed){
                throw new AccessDeniedException("Role " + actualRole +
                        " cannot execute transition requiring " + requiredRole);
            }
        }

        String condType = workflowTransition.getConditionType();
        TransitionCondition cond = conditions.get(condType);

        if(cond == null) throw new IllegalStateException("Unknow Condition Type: " + condType);

        if(!cond.test(task, actor)){
            throw new IllegalStateException("Condition " + condType + " failed for transition " + transitionName);
        }

        task.moveTo(workflowTransition.getToState());

        String actionType = workflowTransition.getActionType();

        WorkflowAction action = actions.get(actionType);

        if(action != null){
            action.execute(task, actor, workflowTransition);
        }

        return task;
    }
}
