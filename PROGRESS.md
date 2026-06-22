# PROGRESS.md

## Current Phase
**Phase 4 — Multi-step workflows** ✅ Complete

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:** Kafka (Bitnami KRaft), async dispatch/result, WorkerService, OrchestratorService, GET status endpoint, EmbeddedKafka tests
- **Phase 2:** Postgres-polling retry with exponential backoff, dead-letter table, RetryPoller, WorkerProperties, ADR-001
- **Phase 3:** Idempotency-Key header, idempotency_keys table, worker-side duplicate result no-op
- **Phase 4:**
  - V5 migration: three-step-workflow seed (steps 1/2/3)
  - Verified: step-1 runs exactly once, step-2 retries in-place, step-3 runs after step-2 succeeds
  - Fixed cross-context H2 interference: each Spring test context now gets unique H2 DB via `${random.uuid}` in URL — prevents RetryPoller from one context interfering with another's RETRYING rows

## In Progress
Nothing.

## Deferred
- Control Room UI — scenario launcher + live SSE log (Phase 5)
- Control Room UI — observability charts (Phase 6)
- Polish, README, v1 tag (Phase 7)
- Dead-letter integration test (isolated Spring context with step-failures=99)
- Crash recovery — explicitly cut from v1

## Open Questions
None.

## Notes for Next Session
- Start Phase 5: React Control Room UI
- Single-page app: scenario launcher (3-4 buttons) + live SSE event log (color-coded, filterable)
- Backend: POST /scenarios/{name} to trigger named scenario (clean-batch, flaky-batch, poison-job, duplicate-test)
- Backend: GET /events/stream (SSE) — push WorkflowInstance state transitions as they happen
- Need SSE endpoint + EventPublisher that hooks into OrchestratorService state changes
- Frontend: minimal React (Vite), fetch SSE via EventSource
- Key decision: SSE vs WebSocket for the live log (SSE is simpler, one-directional fits)
