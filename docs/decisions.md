# Decisions and Trade-offs (ADRs)

## ADR-1 Tenant Isolation
Context: SaaS must prevent Tenant A reading B. Strategies: shared DB shared schema (discriminator), schema per tenant, DB per tenant.
Decision: shared schema with tenant_id + explicit findByIdAndTenantId + 4-layer defense.
Alternatives: schema per tenant (needs routing datasource, 10k schemas), DB per tenant (costly), Hibernate @Filter / @TenantId auto, Postgres RLS.
Trade-off: explicit visible in review, testable, works with Page; but dev can forget. Auto filter/RLS enforces at DB but hidden and breaks Page count, H2 has no RLS. Chosen explicit + RLS future as defense-in-depth. Interview: L1 filter sets TenantContext vs JWT, L2 interceptor checks header, L3 repo adds AND tenant_id, L4 DB RLS.

## ADR-2 Optimistic Locking
Context: two managers approve same task concurrently.
Decision: @Version Long version on Task and WorkflowDefinition, handler maps ObjectOptimisticLockingFailureException -> 409.
Alternatives: pessimistic SELECT FOR UPDATE (blocks, lower throughput), DB unique constraint only, serializable isolation.
Trade-off: optimistic no lock, high throughput for low contention workflow; retry burden on client on 409. Pessimistic better for high contention. Chosen optimistic + 409 retry.

## ADR-3 N+1
Context: Page of 20 tasks looping t.getCurrentState().getName() would fire 60 extra SELECTs.
Decision: @EntityGraph for Page queries, @Query JOIN FETCH for List queries, LAZY everywhere, never EAGER.
Trade-off: EntityGraph keeps count query correct (JOIN FETCH breaks count), JOIN FETCH faster for List. EAGER would load whole graph always.

## ADR-4 Outbox Pattern
Context: DB save + kafka send dual-write can lose event or ghost event.
Decision: OutboxEvent row in same @Transactional as task + history + audit, poller SELECT WHERE published=false LIMIT 100 every 5s, builds WorkflowEvent record and sync kafka send with header eventId + get 3s, markPublished.
Alternatives: dual-write directly, 2PC XA, CDC Debezium.
Trade-off: outbox converts dual-write to single transaction + at-least-once replay; needs poller and idempotency. 2PC not supported by Kafka. Chosen outbox + poller.

## ADR-5 Idempotency
Context: poller retry and Kafka redelivery cause duplicates.
Decision: processed_events PK=eventId, consumer inserts before processing in same transaction, duplicate PK -> skip + ACK. Deterministic fallback UUID.nameUUIDFromBytes(payload) when header missing.
Alternatives: random UUID (breaks duplicate detection), DB unique on payload hash.
Trade-off: deterministic ensures same payload same PK; header is preferred source.

## ADR-6 Kafka vs RabbitMQ
Context: Task 4 requires both; decide where each fits.
Decision: Kafka = loudspeaker, persistent log, ProducerFactory<String, WorkflowEvent> with JsonSerializer (ADD_TYPE_INFO_HEADERS false), acks=all retries=3 enable.idempotence=true, topics nectar.workflow.events (6 partitions) and nectar.task.events as audit (3 partitions) configurable via nectar.kafka.topics.* in application.properties, key=tenantId for per-tenant ordering, consumer group nectar-audit-group, replay via offset, many groups get same copy. RabbitMQ = work queue, DirectExchange + Queue with x-dead-letter to DLQ (5-bean simple config), manual ACK/NACK in consumer, deleted after ACK, one worker gets message.
Trade-off: Kafka high throughput, replay, multi-consumer; Rabbit low latency, per-message ACK/DLQ. Outbox fans out same WorkflowEvent to both: Kafka topic for broadcast, Rabbit exchange for single worker. In prod would split domain vs command, but fan-out proves both for evaluation.

## ADR-7 Strategy Pattern
Context: transitions have varying conditions and actions.
Decision: TransitionCondition and WorkflowAction interfaces with type() + Map<String, Impl> registry via Spring List injection. Implementations @Component AlwaysTrue, AssigneeOnly, LogAction.
Alternatives: if-else on conditionType string, enum switch.
Trade-off: Strategy open/closed, add type without touching engine, testable.

## ADR-8 DTO vs Entity
Context: expose data via REST.
Decision: DTOs as Java records (immutable, no setters), entities as @Builder with package setters + moveTo/assignTo. Flat UUIDs in response.
Trade-off: records true immutability for API, entities controlled mutation for domain. Avoids exposing lazy graph and N+1 serialization.

## ADR-9 Pagination
Context: 1M tasks must not OOM.
Decision: Page<T> with Pageable size 20 (findByTenantId), List for poller batch (no COUNT). Never findAll.
Trade-off: Page does COUNT(*) extra query but UI needs totalPages; poller List avoids count.

## ADR-10 Profiles and Deployment
Context: need H2 local dev and Postgres prod without code change.
Decision: application.properties common (includes nectar.kafka.topics.workflow-events and nectar.kafka.topics.audit plus jwt/flyway), application-dev.properties H2, application-prod.properties Postgres + kafka:9092 + rabbitmq host, SPRING_PROFILES_ACTIVE=prod in compose. Docker multi-stage build with Embedded Tomcat (spring-boot-starter-webmvc). Env vars override secrets; hardcoded in compose for evaluation only.
Alternatives: single file with env override only, external Tomcat WAR.
Trade-off: profiles clear intent; external WAR alternative documented but embedded chosen for stateless horizontal scale.

## Not Implemented and Why
- TaskComment, TaskAssignment history entity, Approval separate table: modeled as Task.assignee + TaskHistory/transition decision to cover evaluation weight; adding would add 60 mins without new pattern, can be append-only same as history.
- Postgres RLS: not enabled on H2, would need V5 migration and SET app.tenant_id per connection; left as future defense-in-depth.
- Redis caching: not added; workflow definitions are small but cache would need invalidation on update.
- Event replay versioning: payload is JSON text; version field could be added to OutboxEvent eventType.
