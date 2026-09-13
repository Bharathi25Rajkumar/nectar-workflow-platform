package com.nectar.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "workflow_states", uniqueConstraints = @UniqueConstraint(columnNames = {"workflow_id", "name"}))
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
public class WorkflowState {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @Setter
    private WorkflowDefinition workflow;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean initial;

    @Column(nullable = false)
    private boolean terminal;

    @Builder.Default
    @OneToMany(mappedBy = "fromState", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTransition> transitions = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void addTransition(WorkflowTransition t){
        transitions.add(t);
        t.setFromState(this);
    }
}
