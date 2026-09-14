CREATE TABLE task_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    task_id UUID NOT NULL REFERENCES tasks(id),
    from_state_id UUID REFERENCES workflow_states(id),
    to_state_id UUID NOT NULL REFERENCES workflow_states(id),
    transition_name VARCHAR(100) NOT NULL,
    actor_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_task_history_tenant_task ON task_history(tenant_id, task_id);
CREATE INDEX idx_task_history_created ON task_history(created_at);

CREATE TABLE audit_records (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id UUID REFERENCES users(id),
    details TEXT,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_tenant ON audit_records(tenant_id);
CREATE INDEX idx_audit_entity ON audit_records(entity_type, entity_id);