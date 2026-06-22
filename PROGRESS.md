# PROGRESS.md

## Current Phase
**Phase 2 — Retry with backoff** ✅ Complete

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:** Kafka (Bitnami KRaft), async dispatch/result, WorkerService, OrchestratorService, GET status endpoint, EmbeddedKafka tests
- **Phase 2:**
  - V3 migration: `next_retry_at`, `max_attempts` on `step_executions`; `dead_letter_entries` table; `flaky-workflow` seed
  - `RetryBackoffCalculator`: exponential backoff (base 2s, cap 5m), capped at attempt 8
  - `RetryPoller`: `@Scheduled` every 1s, queries `step_executions` where RETRYING and `next_retry_at <= now()`, re-dispatches
  - `OrchestratorService.handleStepResult()`: FAILED → schedule retry or dead-letter after `maxAttempts`
  - `DeadLetterEntry` entity + `DeadLetterRepository`
  - `WorkerProperties` (`@ConfigurationProperties`): `failureRate` + `stepFailures` map for per-step deterministic failures
  - ADR-001 written (`docs/decisions/ADR-001-postgres-polling-retries.md`)
  - Integration test: flaky-workflow (step-2 fails 2x then succeeds) → COMPLETED after 2 retries

## In Progress
Nothing.

## Deferred
- Idempotency keys (Phase 3)
- Multi-step retry cycle per step — Phase 4 (Phase 2 already handles retry within a step; Phase 4 adds per-step retry isolation in multi-step flows)
- Control Room UI — scenario launcher + live SSE log (Phase 5)
- Control Room UI — observability charts (Phase 6)
- Polish, README, v1 tag (Phase 7)
- Dead-letter integration test (isolated Spring context needed for step-failures=99 config)
- Crash recovery — explicitly cut from v1

## Open Questions
None.

## Notes for Next Session
- Start Phase 3: idempotency
- Client supplies idempotency key on POST `/workflows/{id}/instances?idempotencyKey=...` (or header)
- Duplicate key within time window → return existing instance instead of creating new
- Worker-side: duplicate dispatch for same (instanceId, stepId, attemptCount) = no-op
- New table: `idempotency_keys` (key, workflow_instance_id, created_at)
- Need to decide: idempotency key as query param vs. header (`Idempotency-Key`)
