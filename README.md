# Nectar Workflow Platform

Production-ready multi-tenant, event-driven workflow management platform.
Java 21, Spring Boot 4.1.1, Spring Security (JWT), Spring Data JPA / Hibernate, Flyway, PostgreSQL (prod) / H2 (dev), Kafka, RabbitMQ, Docker, Embedded Tomcat.

## Quick Start

### Local Dev (no Docker, H2)
```bash
cd D:/Coding/nectar-project/nectar-workflow-platform
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
# app on http://localhost:8080
# H2 console http://localhost:8080/h2-console  jdbc:h2:mem:nectar
```

### Docker Prod-like (Postgres + Kafka + RabbitMQ)
```bash
docker compose up --build
# postgres 5432, kafka 9092/9093, rabbitmq 5672 (mgmt 15672), app 8080
# app uses SPRING_PROFILES_ACTIVE=prod -> application-prod.properties -> postgres:5432, kafka:9092, rabbitmq:rabbitmq
```

### Tests
```bash
./mvnw.cmd test -Dspring.profiles.active=dev
# Tests run: 7, Failures: 0  (6 WorkflowEngineTest + 1 contextLoads)
```

## Project Structure
```
src/main/java/com/nectar/workflow
  config/        SecurityConfig, WebConfig, KafkaConfig, RabbitConfig
  security/      JwtTokenProvider, JwtAuthFilter, TenantInterceptor, TenantContext, AuthPrincipal
  entity/        Tenant, User, Role, Project, WorkflowDefinition, WorkflowState, WorkflowTransition, Task, TaskHistory, AuditRecord, OutboxEvent, ProcessedEvent
  repository/    TenantRepository, UserRepository, ProjectRepository, WorkflowDefinitionRepository, WorkflowStateRepository, WorkflowTransitionRepository, TaskRepository, TaskHistoryRepository, AuditRecordRepository, OutboxEventRepository, ProcessedEventRepository
  engine/        WorkflowEngine, TransitionCondition, AlwaysTrueCondition, AssigneeOnlyCondition, WorkflowAction, LogAction
  service/       AuthService, TaskService, ProjectService, WorkflowService, AuditService, OutboxService
  outbox/        OutboxPoller
  messaging/     DomainEventConsumer (Kafka), TaskRabbitConsumer (RabbitMQ)
  messaging/kafka DomainEvent (record for Kafka JsonSerializer)
  controller/    AuthController, TaskController, ProjectController, WorkflowController
  dtos/          *RequestDto / *ResponseDto as records
  exception/     GlobalExceptionHandler
src/main/resources
  application.properties        (common: jwt, flyway, nectar.kafka.topics.workflow-events, nectar.kafka.topics.task-events)
  application-dev.properties    (H2)
  application-prod.properties   (Postgres + kafka:9092 + rabbitmq)
  db/migration/ V1__init.sql V2__seed.sql V3__history_and_audit.sql V4__outbox.sql
src/test/java   WorkflowEngineTest, NectarWorkflowPlatformApplicationTests
src/test/resources/application.properties (H2 + brokers disabled for contextLoads)
```

## Ports
- 8080 app (Embedded Tomcat)
- 5432 postgres
- 9092/9093 kafka, 2181 zookeeper
- 5672 rabbitmq, 15672 management

## Authentication
All /api/** except /api/auth/** require:
  Authorization: Bearer ***
  X-Tenant-Id: <tenant uuid>

Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111" \
  -d '{"username":"admin","password":"password"}'
# -> {"token":"eyJ...","username":"admin","role":"ADMIN","tenantId":"111..."}
```

Tenant isolation demo:
```bash
# acme admin lists own projects -> 200
curl http://localhost:8080/api/projects -H "Authorization: Bearer ***" -H "X-Tenant-Id: 111..."
# globex token tries to GET acme project id -> 404 not 403 (no leak)
curl http://localhost:8080/api/projects/<acme-id> -H "Authorization: Bearer ***" -H "X-Tenant-Id: 222..."
# wrong X-Tenant-Id vs token -> 403
```

## Kafka / RabbitMQ
- Kafka: ProducerFactory<String, DomainEvent> with JsonSerializer (ADD_TYPE_INFO_HEADERS false), acks=all retries=3 enable.idempotence=true, topics nectar.workflow.events (3 partitions) and nectar.task.events as audit (6 partitions) configurable via nectar.kafka.topics.* in application.properties, key=tenantId for per-tenant ordering
- RabbitMQ: DirectExchange nectar.exchange, Queue nectar.task.queue with x-dead-letter to nectar.task.dlq, 5-bean simple config

## Docs
- [Architecture](docs/architecture.md)
- [Database Design](docs/database-design.md)
- [API Documentation](docs/api.md)
- [Test Cases](docs/test-cases.md)
- [Decisions & Trade-offs](docs/decisions.md)

## Assumptions
- Secrets in docker-compose.yml and application.properties are for evaluation only. Prod injects APP_JWT_SECRET and DB password from Vault/K8s Secrets, not git.
- TIMESTAMP used (UTC via Instant). Prod would use TIMESTAMPTZ; migration would be V5 alter, not edit of V1.
- TaskHistory and AuditRecord cover Approval workflow: approval = transition with requiredRole MANAGER + condition + TaskHistory row. Separate Approval entity, TaskAssignment history, TaskComment are documented as future extensions.
- Kafka and RabbitMQ optional at runtime via @Autowired(required=false) and test auto-startup=false, so local compile/test works without brokers.

## Deployment
Embedded Tomcat via spring-boot-starter-webmvc. No external Tomcat install. WAR packaging is alternative (set packaging war, scope provided for tomcat). Dockerfile multi-stage builds jar then runs with JRE 21. See [Architecture](docs/architecture.md) for Docker details.
