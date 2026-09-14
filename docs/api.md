# API Documentation

Base URL: http://localhost:8080
All /api/** except /api/auth/** require headers: Authorization: Bearer <jwt> and X-Tenant-Id: <uuid>. Missing/invalid -> 401, mismatch X-Tenant-Id vs token -> 403.

## Auth
### POST /api/auth/login
Headers: X-Tenant-Id, Content-Type application/json
Body: {"username":"admin","password":"password"}
Success 200: {"token":"eyJ...","username":"admin","role":"ADMIN","tenantId":"11111111-1111-1111-1111-111111111111"}
Error 401: bad credentials

Example:
```bash
curl -X POST http://localhost:8080/api/auth/login -H "X-Tenant-Id: 11111111-1111-1111-1111-111111111111" -H "Content-Type: application/json" -d '{"username":"admin","password":"password"}'
```

Seed users (password = password): acme admin/manager/employee (111...), globex admin/manager/employee (222...).

## Projects
### POST /api/projects  @PreAuthorize ADMIN,MANAGER
Body CreateProjectRequestDto: {"name":"Alpha","projectKey":"ALPHA","description":"..."}
201 -> ProjectResponseDto {id, tenantId, name, projectKey, description, createdAt, updatedAt}
400 validation (blank/size), 401 unauthenticated, 403 wrong role

### GET /api/projects  paging
Query: ?page=0&size=20&sort=createdAt,desc  (PageableDefault size 20)
200 -> Page<ProjectResponseDto> content, totalElements, totalPages

### GET /api/projects/{id}
200 -> ProjectResponseDto or 404 if wrong tenant (findByIdAndTenantId returns empty -> 404 not 403)

Tenant isolation curl:
```bash
# acme creates
TOKEN_ACME=$(curl -s POST /api/auth/login ... acme admin | jq -r .token)
curl -X POST http://localhost:8080/api/projects -H "Authorization: Bearer $TOKEN_ACME" -H "X-Tenant-Id: 111..." -d '{"name":"Alpha","projectKey":"ALPHA"}'
# globex tries to get acme id
TOKEN_GLOBEX=$(curl -s POST /api/auth/login ... globex admin | jq -r .token)
curl http://localhost:8080/api/projects/<acme-id> -H "Authorization: Bearer $TOKEN_GLOBEX" -H "X-Tenant-Id: 222..."  # -> 404
```

## Workflows
### POST /api/workflows  ADMIN,MANAGER
Body CreateWorkflowRequestDto: {"name":"Approval WF","description":"..."}
201 -> WorkflowResponseDto {id, tenantId, name, description, version, createdAt, updatedAt}

### GET /api/workflows paging
### GET /api/workflows/{id}

### POST /api/workflows/{workflowId}/states  ADMIN,MANAGER
Body CreateStateRequestDto: {"name":"TODO","initial":true,"terminal":false}
201 -> StateResponseDto {id, workflowId, name, initial, terminal}

### POST /api/workflows/states/{fromStateId}/transitions  ADMIN,MANAGER
Body CreateTransitionRequestDto: {"toStateId":"<uuid>","name":"start","requiredRole":"MANAGER","conditionType":"ALWAYS_TRUE","actionType":"LOG"}
201 -> TransitionResponseDto {id, fromStateId, toStateId, name, requiredRole, conditionType, actionType}
Error 404 if workflow/state not in tenant, 400 if duplicate name per fromState.

Sequence to configure data-driven workflow:
```bash
WID=$(curl -s POST /api/workflows ... -d '{"name":"WF"}' | jq -r .id)
TODO=$(curl -s POST /api/workflows/$WID/states ... -d '{"name":"TODO","initial":true}' | jq -r .id)
DONE=$(curl -s POST /api/workflows/$WID/states ... -d '{"name":"DONE","initial":false,"terminal":true}' | jq -r .id)
curl -X POST http://localhost:8080/api/workflows/states/$TODO/transitions ... -d '{"toStateId":"'"$DONE"'","name":"complete","requiredRole":"EMPLOYEE","conditionType":"ASSIGNEE_ONLY","actionType":"LOG"}'
```

## Tasks
### POST /api/tasks  ADMIN,MANAGER,EMPLOYEE
Body CreateTaskRequestDto: {"projectId":"<uuid>","workflowId":"<uuid>","title":"Task 1","description":"..."}
201 -> TaskResponseDto {id, tenantId, projectId, workflowId, currentState (name), title, description, assigneeId, version, createdAt, updatedAt}
400 if project/workflow not in tenant or initial state missing, 404 project/workflow not found.

### GET /api/tasks paging  ?page=0&size=20
### GET /api/tasks/project/{projectId} paging
### GET /api/tasks/{id}

### POST /api/tasks/{id}/transitions  ADMIN,MANAGER,EMPLOYEE
Body TransitionRequestDto: {"transitionName":"start"}
200 -> TaskResponseDto with new currentState and version bump
Errors: 404 transition not from current state, 403 role denied (AccessDeniedException -> 403 via GlobalExceptionHandler), 400 condition failed (IllegalStateException), 409 conflict if version stale (ObjectOptimisticLockingFailureException -> 409).

Example 409:
```bash
# two managers read version 5, first POST transitions succeeds -> version 6
# second POST with stale version -> 409 Conflict: task was modified by another user, retry
```

### GET /api/tasks/{id} includes version for retry.

## Error Codes
400 BAD_REQUEST validation or IllegalStateException (condition failed)
401 UNAUTHORIZED missing/invalid JWT
403 FORBIDDEN X-Tenant-Id mismatch or @PreAuthorize role
404 NOT_FOUND tenant-isolated find returns empty
409 CONFLICT optimistic locking
500 fallback

## Not Implemented
- GET /api/tasks/{id}/history (can be added via TaskHistoryRepository.findByTenantIdAndTaskId with paging, no new transaction)
- POST /api/tasks/{id}/comments, /api/tasks/{id}/assign - modeled as Task.assignee + TaskHistory; can be added as TaskComment entity.

## DTOs
All Request/Response as Java records (immutable, no setters). Entity uses Builder + package setters for controlled mutation. Flat UUIDs in response to avoid N+1 serialization.

