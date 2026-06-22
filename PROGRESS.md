# PROGRESS.md

## Current Phase
**Phase 1 — Kafka transport, single step, happy path** ✅ Complete

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:**
  - Kafka (Bitnami KRaft, no Zookeeper) added to docker-compose
  - `StepDispatchMessage` / `StepResultMessage` records + `Topics` constants
  - `WorkerService` — `@KafkaListener` on dispatch topic, simulates execution (configurable failure rate), publishes result
  - `OrchestratorService` rewritten — `submit()` creates instance + dispatches first step async; `handleStepResult()` advances to next step or marks COMPLETED/FAILED; `getStatus()` for polling
  - `POST /workflows/{id}/instances` → 202 Accepted (async)
  - `GET /workflows/instances/{id}` → current status
  - Tests use `@EmbeddedKafka` + Awaitility polling
  - Worker failure rate configurable via `driftwood.worker.failure-rate` (default 0.0)
  - Design decision: worker as `@Component` in same app (not separate module) — simpler for demo, split is mechanical later

## In Progress
Nothing.

## Deferred
- Retry with exponential backoff + dead-letter (Phase 2)
- Idempotency keys (Phase 3)
- Multi-step retry cycle per step (Phase 4 adds this; Phase 1 already advances steps)
- Control Room UI — scenario launcher + live SSE log (Phase 5)
- Control Room UI — observability charts (Phase 6)
- Polish, README, v1 tag (Phase 7)
- Crash recovery — explicitly cut from v1 (see DESIGN.md)

## Open Questions
None.

## Notes for Next Session
- Start Phase 2: retry with exponential backoff
- Write ADR-001 (Postgres-polling retries) BEFORE writing any Phase 2 code — this is the centerpiece design decision
- Phase 2 intercepts the FAILED path in `OrchestratorService.handleStepResult()`: instead of marking FAILED, write `next_retry_at` + increment `attempt_count`, mark RETRYING
- Need: `next_retry_at` + `attempt_count` columns on `step_executions` (Flyway V3), a scheduled poller (`@Scheduled`), exponential backoff calc, max attempt cap, dead-letter path
- Test scenario: configure trivial-workflow-step-2 to fail twice then succeed (needs per-step failure config or a new seed workflow)
