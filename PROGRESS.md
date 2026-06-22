# PROGRESS.md

## Current Phase
**Phase 3 — Idempotency** ✅ Complete

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:** Kafka (Bitnami KRaft), async dispatch/result, WorkerService, OrchestratorService, GET status endpoint, EmbeddedKafka tests
- **Phase 2:** Postgres-polling retry with exponential backoff, dead-letter table, RetryPoller, WorkerProperties, ADR-001
- **Phase 3:**
  - V4 migration: `idempotency_keys` table (key → workflow_instance_id)
  - `POST /workflows/{id}/instances` accepts `Idempotency-Key` header (optional)
  - Duplicate key within any window returns existing instance (no new instance created)
  - Worker-side: duplicate result for already-terminal StepExecution is a no-op
  - Test: same key twice → same instance ID returned

## In Progress
Nothing.

## Deferred
- Multi-step per-step retry cycle isolation (Phase 4)
- Control Room UI — scenario launcher + live SSE log (Phase 5)
- Control Room UI — observability charts (Phase 6)
- Polish, README, v1 tag (Phase 7)
- Dead-letter integration test (needs separate Spring context with step-failures=99)
- Crash recovery — explicitly cut from v1

## Open Questions
None.

## Notes for Next Session
- Phase 4 already partially works: orchestrator advances steps after each success
- Phase 4 focus: verify per-step retry cycle is isolated (step-2 failure doesn't restart step-1)
- May need: workflow seed with 3 steps where step-2 fails N times, verify step-1 runs exactly once
- Consider adding `GET /workflows/instances/{id}` response to include step-level retry count
