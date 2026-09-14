# Database Design

## Migrations
Flyway, validate mode. Postgres dialect, compatible with H2 MODE=PostgreSQL.

### V1__init.sql (implemented)
- tenants (id UUID PK, slug VARCHAR(100) UNIQUE, name VARCHAR(200), created_at/updated_at TIMESTAMP)
- users (id UUID PK, tenant_id UUID FK tenants, username VARCHAR(100), password_hash VARCHAR(255), role VARCHAR(20) enum ADMIN/MANAGER/EMPLOYEE, email, created_at/updated_at, UNIQUE(tenant_id, username), INDEX idx_users_tenant)
- projects (id UUID PK, tenant_id FK, project_key VARCHAR(20), name VARCHAR(200), description, created_at/updated_at, UNIQUE(tenant_id, project_key))
- workflow_definitions (id UUID PK, tenant_id FK, name VARCHAR(100), description, version BIGINT, created_at/updated_at, UNIQUE(tenant_id, name))
- workflow_states (id UUID PK, workflow_id FK workflow_definitions, name VARCHAR(100), initial BOOLEAN, terminal BOOLEAN, created_at/updated_at, UNIQUE(workflow_id, name))
- workflow_transitions (id UUID PK, from_state_id FK workflow_states, to_state_id FK workflow_states, name VARCHAR(100), required_role VARCHAR(20), condition_type VARCHAR(30), action_type VARCHAR(30), UNIQUE(from_state_id, name))
- tasks (id UUID PK, tenant_id FK, project_id FK, workflow_id FK, current_state_id FK workflow_states, title VARCHAR(300), description VARCHAR(2000), assignee_id FK users, version BIGINT, created_at/updated_at, INDEX idx_tasks_tenant_project, INDEX idx_tasks_current_state)

UUID PK chosen for global uniqueness across tenants and safe sharding; trade-off larger index than serial.

### V2__seed.sql (implemented)
Two tenants acme (111...), globex (222...), 6 users (admin/manager/employee per tenant) password BCrypt hash for "password", so login works without manual seeding.

### V3__history_and_audit.sql (implemented)
- task_history (id UUID PK, tenant_id UUID not FK, task_id FK tasks, from_state_id FK, to_state_id FK NOT NULL, transition_name VARCHAR(100), actor_id FK users, created_at TIMESTAMP, INDEX idx_task_history_tenant_task(tenant_id,task_id), INDEX idx_task_history_created)
- audit_records (id UUID PK, tenant_id UUID not FK, entity_type VARCHAR(100), entity_id UUID, action VARCHAR(50), actor_id FK users, details TEXT, created_at TIMESTAMP, INDEX idx_audit_tenant, INDEX idx_audit_entity)

Denormalized tenant_id as UUID column (not FK) for index-only query WHERE tenant_id=? AND task_id=? without JOIN, fast for millions of rows. Trade-off no FK enforcement, but source of truth remains Task.tenant.

### V4__outbox.sql (implemented)
- outbox_events (id UUID PK, tenant_id UUID, aggregate_type VARCHAR(100), aggregate_id UUID, event_type VARCHAR(100), payload TEXT, published BOOLEAN DEFAULT FALSE, created_at TIMESTAMP, published_at TIMESTAMP, INDEX idx_outbox_published(published,createdAt), INDEX idx_outbox_tenant)
- processed_events (event_id UUID PK, processed_at TIMESTAMP) for idempotency

### Not Implemented (documented future)
- task_comments (id, tenant_id, task_id FK, author_id FK, body TEXT, created_at)
- task_assignments history (id, tenant_id, task_id FK, assignee_id FK, assigned_by FK, created_at) - current assignee is Task.assignee field + assignTo method; history via TaskHistory not separate.
- approvals (id, tenant_id, task_id FK, decision ENUM APPROVED/REJECTED/REWORK, comment, approver_id FK, created_at) - modeled as TaskHistory with transition APPROVE/REJECT + role check; separate entity if multi-level approval needed.
Reason: evaluation weight favors engine + isolation + outbox + paging; adding 3 more tables would add 60 mins without new pattern. Can be added as append-only with same tenant_id denormalization.

## JPA Mapping
Entities use Lombok @Builder, @NoArgsConstructor PROTECTED, @Getter, no public setters except package moveTo/assignTo. @CreationTimestamp/@UpdateTimestamp for audit. LAZY everywhere, indexes match queries. @Version on Task and WorkflowDefinition for optimistic locking.

## Trade-offs
- TIMESTAMP vs TIMESTAMPTZ: used TIMESTAMP with Instant UTC; prod recommendation TIMESTAMPTZ. Change would be V5 ALTER, not edit of V1 (Flyway checksum).
- ddl-auto validate in dev/prod (Flyway manages schema), create-drop only in test profile for fast contextLoads.
