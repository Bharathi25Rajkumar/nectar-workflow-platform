package com.nectar.workflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "workflow_transitions", uniqueConstraints = @UniqueConstraint(columnNames = {"from_state_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class WorkflowTransition {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_state_id", nullable = false)
    @Setter
    private WorkflowState fromState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_state_id", nullable = false)
    private WorkflowState toState;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role requiredRole;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String conditionType = "ALWAYS_TRUE";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String actionType = "LOG";


}
