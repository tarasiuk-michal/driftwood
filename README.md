# Driftwood

A Kafka-backed workflow orchestrator with retries, dead-lettering, and idempotency — paired with a live "control room" UI that fires chaos scenarios at it and shows the system recover in real time.

Jobs that get knocked off course (failures, retries) still reliably make it to shore.

---

## Why this exists

Most workflow systems hide the interesting stuff — retries happen invisibly, failure modes are hard to trigger on demand, and the "reliable message processing" pitch is easy to make but harder to demonstrate.

Driftwood is built to make the hard parts visible: fire a batch of flaky jobs, watch some fail, watch the retry backoff kick in, watch them all eventually land. Or fire a poison job and watch it get dead-lettered after exhausting its attempts. The control room shows every state transition as it happens over a live SSE stream.

---

## Architecture

```
┌──────────────┐     ┌──────────┐     ┌──────────────────┐
│  Control     │────▶│  REST    │────▶│  Orchestrator    │
│  Room UI     │◀────│  API     │◀────│  (state machine) │
│  (React)     │ SSE │ (Spring) │     └──────────┬───────┘
└──────────────┘     └──────────┘                │
                                          ┌──────┴───────┐
                                          │   Postgres   │
                                          │ (source of   │
                                          │   truth)     │
                                          └──────┬───────┘
                                                 │
                                    ┌────────────┴─────────────┐
                                    │         Kafka            │
                                    │  dispatch / result topics│
                                    └────────────┬─────────────┘
                                                 │
                                          ┌──────┴───────┐
                                          │   Worker     │
                                          │ (simulated   │
                                          │  step exec)  │
                                          └──────────────┘
```

**Core principle:** Postgres is the source of truth for all state. Kafka is pure transport — dispatch and result messages only. This is what makes retry reasoning clean.

---

## The retry design (the interesting part)

On step failure, the orchestrator does **not** republish to Kafka immediately. Instead it writes `next_retry_at` to the `step_executions` row and marks the step `RETRYING`. A scheduled poller (every 1s) scans for rows where `next_retry_at <= now()` and re-dispatches them to the dispatch topic.

**Why Postgres-polling over Kafka delay-topics:**

Retrying via Kafka means either blocking the consumer thread (stalling every other message on that partition behind the slow retry) or building a tiered delay-topic system (e.g. `retry-2s`, `retry-30s`, `retry-5m`). Polling Postgres reuses the state that is already the source of truth, keeps the dispatch path non-blocking, and makes retry timing exact rather than approximate.

Tradeoff: introduces a polling component and a latency floor tied to the poll interval (1s). For a workflow system where retries are measured in seconds or minutes, this is acceptable.

See [`docs/decisions/ADR-001-postgres-polling-retries.md`](docs/decisions/ADR-001-postgres-polling-retries.md).

Backoff: exponential with base 2s, capped at 5 minutes.

```
attempt 1 → wait 4s
attempt 2 → wait 8s
attempt 3 → dead-lettered
```

---

## How to run

**Prerequisites:** Docker, Java 21, Node 18+

```bash
# 1. Start Postgres + Kafka
docker compose up -d

# 2. Start the backend
./mvnw spring-boot:run

# 3. Start the UI (separate terminal)
cd ui && npm install && npm run dev
```

Open http://localhost:5173.

### Scenarios

| Button | What it does |
|--------|-------------|
| **Clean Batch** | Submits 5 × trivial-workflow (2 steps, always succeeds) |
| **Flaky Batch** | Submits 5 × flaky-workflow (step-2 forced to fail, retry, succeed) |
| **Poison Job** | Submits 1 × poison-workflow (step-2 always fails → dead-lettered) |
| **Duplicate Test** | Submits trivial-workflow twice with the same idempotency key — second call returns the first instance |

---

## API

```
POST /workflows/{workflowId}/instances        → 202 { id, status, ... }
GET  /workflows/instances/{instanceId}        → 200 { id, status, steps: [...] }
POST /scenarios/{name}                        → 202 { scenario, instanceIds }
GET  /events/stream                           → SSE stream of state transitions
GET  /metrics/summary                         → { inFlight, completed, deadLettered, retrying, avgStepLatencyMs }
```

---

## Tests

```bash
# All tests (H2 in-memory + EmbeddedKafka, no Docker needed)
./mvnw test

# Single class
./mvnw test -Dtest=WorkflowControllerTest
```

---

## Domain model

- `Workflow` + `Step` — definitions (seeded via Flyway, not created at runtime)
- `WorkflowInstance` + `StepExecution` — runtime state created per invocation
- `DeadLetterEntry` — written when a step exhausts all retry attempts
- `IdempotencyKey` — maps client-supplied key to existing instance

---

## Deliberately out of scope for v1

**Crash recovery:** if the orchestrator process dies mid-flight, in-progress `WorkflowInstance` rows remain in `RUNNING` state with no one advancing them. The fix is a startup scan for stale running instances older than a heartbeat timeout, plus an at-least-once delivery guarantee on the result consumer. This is a well-defined forward item, not a hidden gap — the Postgres-source-of-truth design makes it straightforward to add.
