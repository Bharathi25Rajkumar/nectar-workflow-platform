# Test Cases

## Run
```bash
./mvnw.cmd test -Dspring.profiles.active=dev
# or
./mvnw.cmd test '-Dspring.profiles.active=dev'
# Tests run: 7, Failures: 0 (6 WorkflowEngineTest + 1 contextLoads)
```

Test profile: src/test/resources/application.properties uses H2 jdbc:h2:mem:nectar_test, ddl-auto create-drop, flyway disabled, jwt secret dummy, kafka.admin.auto-create=false, kafka.listener.auto-startup=false, rabbitmq.listener.simple.auto-startup=false so contextLoads does not require brokers.

## WorkflowEngineTest (6 tests, pure unit, no Spring)
File: src/test/java/com/nectar/workflow/engine/WorkflowEngineTest.java
No DB, constructs entities via Builder, adds transitions via todo.addTransition, calls engine.advance directly. LogAction(null) with null-check avoids repo.

| Test | Input | Expected | Proves |
|------|-------|----------|--------|
| advance_validTransition_succeeds | TODO --start(MANAGER, ALWAYS_TRUE)--> IN_PROGRESS, actor manager | returns IN_PROGRESS | Strategy dispatch + happy path |
| advance_invalidTransition_throws404 | no transition named unknown on currentState | ResourceNotFoundException | invalid graph -> 404 |
| advance_roleDenied_throws403 | transition requires MANAGER, actor EMPLOYEE | AccessDeniedException -> 403 | coarse role gate, GlobalExceptionHandler handlesForbidden |
| advance_adminCanDoManagerTransition | same as above, actor ADMIN | returns IN_PROGRESS | ADMIN bypass logic actual==required \|\| actual==ADMIN |
| advance_assigneeOnlyFails_whenNotAssignee | IN_PROGRESS --complete(MANAGER, ASSIGNEE_ONLY)--> DONE, task assignee employee, actor manager | IllegalStateException Condition ASSIGNEE_ONLY failed | fine-grained condition, role passes but condition fails |
| advance_assigneeOnlySucceeds_whenAssignee | same, actor employee (is assignee) | returns DONE | assignee check equals |

These 6 cover Strategy (AlwaysTrue vs AssigneeOnly), role hierarchy, validation, and state change. Add transition via workflowState.addTransition ensures fromState bound correctly.

## NectarWorkflowPlatformApplicationTests.contextLoads
Loads full Spring context with H2 test profile. Verifies wiring: SecurityConfig, JwtTokenProvider, WorkflowEngine Map registry, KafkaConfig NewTopic beans (but admin not creating), RabbitConfig bindings, outbox poller. Hibernate ddl create-drop creates 11 tables with indexes and FKs. No brokers needed.

## Not Implemented (future)
- Tenant isolation integration test with TestContainers Postgres: create acme and globex projects, verify findByIdAndTenantId returns 404 cross-tenant. Not added due to time; can be done with @SpringBootTest + @Transactional + TenantContext.set.
- Optimistic locking concurrency test: two threads advance same task version 5 -> one succeeds, one gets ObjectOptimisticLockingFailureException -> 409. Can be done with @Transactional + CountDownLatch.
- Outbox poller integration: save task -> assert outbox row published=false, run poller.poll(), assert published=true.
- Kafka/Rabbit idempotency test: send same eventId twice -> processed_events second insert throws duplicate -> second handled as duplicate skipped.

## Coverage
Implemented tests prove core engine rules without DB. Context load proves wiring and schema. Deferred tests above would bring total to ~15 and cover reliability and isolation end-to-end (estimated 45 mins).
