# job-scheduling-platform

**Real-Time Multi-Tenant Job Scheduling and Execution Platform** — a Spring Boot 3.2 backend for defining jobs per tenant, dispatching work through RabbitMQ priority queues, tracking executions in PostgreSQL, coordinating cron deduplication with Redis, and streaming progress over STOMP WebSockets.

## Architecture

```
┌─────────────┐     REST/WS      ┌──────────────────────┐
│   Clients   │◄───────────────►│  scheduler-service   │
│  (external) │                  │  (API + scheduler)   │
└─────────────┘                  └──────────┬───────────┘
                                            │
              ┌─────────────────────────────┼─────────────────────────────┐
              │                             │                             │
              ▼                             ▼                             ▼
      ┌───────────────┐            ┌────────────────┐            ┌─────────────┐
      │  PostgreSQL   │            │    RabbitMQ    │            │    Redis    │
      │  jobs/execs   │            │ queues/exch.   │            │ locks/reg.  │
      └───────────────┘            └────────────────┘            └─────────────┘
```

## System design concepts

### Competing consumers with priority queues

Work is published to a **direct exchange** (`job.execution`) with routing key `job.dispatch`. The bound queue `job.work.queue` declares `x-max-priority=10`, so higher-priority jobs are consumed first when multiple consumers (`@RabbitListener(concurrency = "3")`) compete for messages.

### Dead Letter Exchange (DLX) and delayed retry (TTL flow)

Failed executions that are still retryable are published to the **direct exchange** `job.retry` and land in `job.retry.queue`. That queue is declared with:

- `x-dead-letter-exchange` → `job.execution`
- `x-dead-letter-routing-key` → `job.dispatch`

Each retry message carries a **per-message TTL** (`expiration` in milliseconds). When the TTL elapses, RabbitMQ **dead-letters** the message back to `job.execution`, which routes it again to `job.work.queue` — implementing delay without a dedicated delayed-message plugin. After attempts are exhausted, the worker routes the payload to the **DLQ** (`job.dlq` via `job.dlx`).

### Distributed locking for cron deduplication

`CronSchedulerService` runs every 15 seconds. Before firing a due job, it acquires a Redis lock with `SETNX`-style semantics (`setIfAbsent`) keyed by `cron:lock:{jobId}:{nextRunAt}` with a short TTL, so multiple service instances do not enqueue duplicate scheduled runs for the same cron tick.

### Fan-out for event broadcasting

`ExecutionService` publishes `ExecutionEvent` to the **fanout exchange** `job.events`. Every bound queue receives a copy; `ExecutionEventConsumer` forwards events to STOMP subscribers on `/topic/executions/{tenantId}` so UIs can observe live state without polling.

### Multi-tenancy isolation

Jobs and executions are keyed by `tenantId` at the API (`/api/v1/tenants/{tenantId}/...`) and in JPA entities. Repositories scope queries by tenant. Execution detail fetches verify the path tenant matches the execution’s tenant.

### Worker heartbeat protocol

The scheduler JVM periodically publishes `WorkerHeartbeat` to `worker.heartbeat.exchange` → `worker.heartbeat`. `HeartbeatConsumer` persists compact heartbeat payloads in Redis with a **30 second TTL**. `WorkerController` and dashboard stats expose **active worker count** derived from live Redis keys.

## RabbitMQ topology (ASCII)

```
                         ┌─────────────────┐
            ┌──────────►│ job.execution   │ (direct)
            │           │  rk: job.dispatch
            │           └────────┬────────┘
            │                    │
            │                    ▼
            │           ┌─────────────────┐
            │           │ job.work.queue  │  x-max-priority=10
            │           └────────▲────────┘
            │                    │
┌───────────┴──────────┐         │  consumers (x3)
│ job.retry (direct)   │         │
│  rk: job.retry       │         │
└───────────┬──────────┘         │
            │                    │
            ▼                    │
   ┌─────────────────┐           │
   │ job.retry.queue │  DLX ─────┘ (TTL expires → republish)
   └─────────────────┘

   ┌─────────────────┐     ┌─────────────────┐
   │ job.dlx (dir.) │────►│ job.dlq         │
   └─────────────────┘     └─────────────────┘

   ┌─────────────────┐
   │ job.events      │ (fanout) ──► job.events.queue ──► WebSocket bridge
   └─────────────────┘

   ┌──────────────────────────┐
   │ worker.heartbeat.exchange │ (direct, rk: heartbeat) ──► worker.heartbeat
   └──────────────────────────┘
```

## API documentation

Interactive docs: **http://localhost:8081/swagger-ui.html** (OpenAPI: `/api-docs`).

### curl examples

Create a tenant:

```bash
curl -s -X POST http://localhost:8081/api/v1/tenants \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"acme","name":"Acme Corp","executionQuota":5000}'
```

Create an HTTP job:

```bash
curl -s -X POST http://localhost:8081/api/v1/tenants/acme/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId":"acme",
    "name":"Health check",
    "description":"Ping external URL",
    "jobType":"HTTP_CALL",
    "configuration":{
      "url":"https://httpbin.org/get",
      "method":"GET"
    },
    "priority":"HIGH",
    "timeoutSeconds":60,
    "maxRetries":2,
    "tags":["http","monitoring"]
  }'
```

Trigger manually:

```bash
curl -s -X POST http://localhost:8081/api/v1/tenants/acme/jobs/{jobId}/trigger
```

List executions:

```bash
curl -s "http://localhost:8081/api/v1/tenants/acme/executions?page=0&size=20"
```

Dashboard stats:

```bash
curl -s http://localhost:8081/api/v1/tenants/acme/executions/stats
```

Workers:

```bash
curl -s http://localhost:8081/api/v1/workers
curl -s http://localhost:8081/api/v1/workers/count
```

## How to run

From the project root:

```bash
docker compose up --build
```

Services: **PostgreSQL** (5432), **RabbitMQ** (5672, management UI **15672**), **Redis** (6379), **scheduler-service** (8081).

Default datasource and messaging hosts in `application.yml` match Docker service names (`postgres`, `rabbitmq`, `redis`).

## Job type configuration examples

**HTTP_CALL** — `configuration` map:

```json
{
  "url": "https://api.example.com/v1/ping",
  "method": "POST",
  "headers": { "Authorization": "Bearer ***" },
  "body": "{\"check\":true}"
}
```

**SHELL_SCRIPT** (simulated):

```json
{ "script": "echo batch-export && sleep 1" }
```

**SQL_QUERY** (simulated):

```json
{ "query": "SELECT COUNT(*) FROM orders WHERE status = 'OPEN'" }
```

**PYTHON_SCRIPT** (simulated):

```json
{ "script": "print('etl done')" }
```

**Cron** (optional): six-field Spring cron (seconds first), e.g. `0 0 * * * *` (every hour at minute 0). `nextRunAt` is computed with `CronExpression`.

## Execution state machine

```
                    ┌──────────┐
                    │ PENDING  │
                    └────┬─────┘
                         │ dispatch
                         ▼
                    ┌──────────┐
         ┌─────────│  QUEUED  │─────────┐
         │         └────┬─────┘         │
         │              │ consume       │
         │              ▼               │
         │         ┌──────────┐          │
         │         │ RUNNING  │          │
         │         └────┬─────┘          │
         │    success   │    failure     │
         │              │                │
         │              ▼                ▼
         │         ┌──────────┐    ┌──────────┐
         │         │COMPLETED │    │ RETRYING │──► (TTL retry queue) ──► QUEUED
         │         └──────────┘    └────┬─────┘
         │                              │ retries exhausted
         │                              ▼
         │                         ┌──────────────┐
         │                         │DEAD_LETTERED │
         │                         └──────────────┘
         │ timeout
         ▼
    ┌───────────┐
    │ TIMED_OUT │
    └───────────┘
```

## Modules

| Module | Artifact | Role |
|--------|----------|------|
| **common** | `common` | Shared DTOs, enums, events, `JobSchedulerException` |
| **scheduler-service** | `scheduler-service` | REST API, JPA, RabbitMQ, Redis, WebSocket, actuator, OpenAPI |

**Java 17**, **Spring Boot 3.2.12**, **Lombok 1.18.42** (pinned in parent `dependencyManagement` and `maven-compiler-plugin` annotation processor paths).

## WebSocket (STOMP)

- Endpoint: `ws://localhost:8081/ws`
- Application prefix: `/app`
- Broker prefix: `/topic`
- Execution events: `/topic/executions/{tenantId}`
- Logs: `/topic/logs/{executionId}`

## Future enhancements

- Persistent STOMP broker (e.g. Redis or Rabbit STOMP relay) for horizontal WebSocket scale-out
- Quartz / external cron store for large-scale schedule fan-out
- Pluggable job isolation (sandboxed processes/containers) instead of simulated shell/Python/SQL
- Policy engine for tenant quotas and rate limits enforced before dispatch
- Audit log and execution artifact storage (S3-compatible) for large outputs
- OAuth2 / JWT multi-tenant security at the API edge

---

**License:** Proprietary / define as needed for your organization.
