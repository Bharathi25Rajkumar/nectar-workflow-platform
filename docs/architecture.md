# Architecture

## Overview
Multi-tenant, event-driven workflow platform for 10k tenants, 1M tasks, millions of history records. Stateless app, horizontal scale, Postgres indexes, Kafka partitions, RabbitMQ work queues.

## System Context
```
Client -> Load Balancer -> App x N (stateless) -> Postgres 16
                            |-> Kafka (nectar.task.events, 12 partitions, key=tenantId)
                            |-> RabbitMQ (nectar.exchange -> nectar.task.queue -> DLQ nectar.task.dlq)
                            |-> Outbox Poller (fixedDelay 5s)
```

## Layered Request Flow
JwtAuthFilter (OncePerRequestFilter) -> TenantInterceptor (HandlerInterceptor) -> Controller (@PreAuthorize) -> Service (@Transactional) -> Engine (Strategy) -> Repository (EntityGraph) -> DB

## Multi-Tenancy
**Strategy:** Shared Database, Shared Schema with discriminator tenant_id. Chosen over Schema-per-tenant (10k schemas = operational burden, migration complexity) and Database-per-tenant (cost, backup). Shared schema cheapest and pagination handles 1M rows.

**Isolation 4 layers (implemented 1-3, 4 documented as future):**
- L1 JwtAuthFilter: validates Bearer signature, parses tenantId/userId/role, compares X-Tenant-Id header vs token tenantId else 403. Sets TenantContext (ThreadLocal) and SecurityContext with AuthPrincipal(tenantId,userId,username,role).
- L2 TenantInterceptor: rejects missing X-Tenant-Id, clears TenantContext after completion to prevent cross-request leak.
- L3 Repository: explicit findByIdAndTenantId(id, TenantContext.get()). Never findById. States/Transitions use @Query JOIN s.workflow.tenant.id. Wrong tenant -> 0 rows -> ResourceNotFoundException -> 404 not 403 (avoids tenant existence oracle).
- L4 DB RLS (not implemented): Postgres policy tenant_id = current_setting('app.tenant_id'). Documented as defense-in-depth; not enabled on H2. Future if needed.

Trade-off: explicit is visible in review and testable, but dev can forget. RLS enforces at DB but is hidden and breaks Page count.

## Workflow Engine
Data-driven via API, not hard-coded. WorkflowDefinition (aggregate root) -> WorkflowState -> WorkflowTransition. Created by POST /api/workflows, POST /api/workflows/{id}/states, POST /api/workflows/states/{fromId}/transitions.

WorkflowEngine.advance(Task, User, transitionName) @Transactional:
1. resolve outgoing transition from currentState.getTransitions()
2. check requiredRole (ADMIN bypass, else exact match)
3. lookup TransitionCondition by type() from Map<String,TransitionCondition> (Strategy)
4. if !test -> IllegalStateException
5. task.moveTo(toState) (package setter, dirty checking)
6. execute WorkflowAction by type() (LogAction persists TaskHistory)

Strategy: TransitionCondition (AlwaysTrueCondition, AssigneeOnlyCondition) and WorkflowAction (LogAction) as @Component, registry Map built from List injection. New type = new @Component, no engine change (open/closed).

## Concurrency
@Version Long version on Task and WorkflowDefinition. Hibernate generates UPDATE ... WHERE version=? . 0 rows -> OptimisticLockException -> ObjectOptimisticLockingFailureException -> GlobalExceptionHandler 409 Conflict with retry message. Alternative pessimistic SELECT FOR UPDATE would block and reduce throughput; optimistic fits low-contention workflow.

## Persistence and N+1
All @ManyToOne LAZY. Fix: @EntityGraph for Page queries (keeps count query correct), @Query JOIN FETCH for List queries. Pagination via Page<T> findByTenantId with Pageable size 20; never findAll. Poller uses List not Page to avoid COUNT.

## Event-Driven
Outbox: TaskService create/transition saves Task + TaskHistory + AuditRecord + OutboxEvent in same @Transactional. If Kafka down, row stays published=false. Poller polls SELECT WHERE published=false ORDER BY createdAt LIMIT 100, sends to Kafka with header eventId=outbox.id (sync get 3s so failure keeps published=false) and to RabbitMQ with same header, then markPublished().

Kafka: topic nectar.task.events 12 partitions, key=tenantId for per-tenant ordering, consumer group nectar-audit-group scales to 12 instances, persistent log with replay via offset.

RabbitMQ: DirectExchange nectar.exchange, Queue nectar.task.queue with x-dead-letter-exchange to DLQ, manual ACK (basicAck tag,false after idempotency+logic, basicNack tag,false,false to DLQ on failure). Work queue semantics, deleted after ACK.

Idempotency: processed_events PK=eventId. Consumer inserts before processing in same transaction; duplicate PK -> DataIntegrityViolationException -> skip and ACK. Deterministic fallback UUID.nameUUIDFromBytes(payload) when header absent.

## Deployment
Embedded Tomcat via spring-boot-starter-webmvc (spring-boot-maven-plugin jar). Dockerfile multi-stage: build stage mvnw dependency:go-offline then package -DskipTests, runtime stage JRE + app.jar, EXPOSE 8080. Profiles: application.properties common, application-dev.properties H2, application-prod.properties Postgres+kafka:9092+rabbitmq. SPRING_PROFILES_ACTIVE=prod in compose. Secrets in compose for evaluation only.

Compose: postgres:16 healthcheck pg_isready, zookeeper, kafka healthcheck kafka:9092, rabbitmq:3-management, app depends_on service_healthy.

## Scalability Notes (implemented)
- Indexes: idx_tasks_tenant_project, idx_tasks_current_state, idx_users_tenant, idx_task_history_tenant_task, idx_outbox_published, idx_audit_tenant
- Hikari pooling, stateless, horizontal scale
- Kafka 12 partitions, Rabbit competing consumers
- Future: Redis for workflow definition cache, read replica, partitioning by tenant_id
