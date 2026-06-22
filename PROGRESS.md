# PROGRESS.md

## Current Phase
**Phase 5 — Control Room UI, part 1** ✅ Complete

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:** Kafka (Bitnami KRaft), async dispatch/result, WorkerService, OrchestratorService, GET status endpoint, EmbeddedKafka tests
- **Phase 2:** Postgres-polling retry with exponential backoff, dead-letter table, RetryPoller, WorkerProperties, ADR-001
- **Phase 3:** Idempotency-Key header, idempotency_keys table, worker-side duplicate result no-op
- **Phase 4:** three-step-workflow seed, MultiStepWorkflowTest, fixed cross-context H2 interference
- **Phase 5:**
  - V6 migration: poison-workflow seed (step-failures=99 → hits maxAttempts → dead-lettered)
  - `WorkflowEvent` + `WorkflowEventBus`: events published post-commit via `@TransactionalEventListener(AFTER_COMMIT)`
  - `SseController`: GET /events/stream — SSE push to browser
  - `ScenarioController`: POST /scenarios/{clean-batch|flaky-batch|poison-job|duplicate-test}
  - `WebConfig`: CORS for localhost:5173 (Vite dev server)
  - `OrchestratorService`: publishes SUBMITTED, STEP_DISPATCHED, STEP_COMPLETED, RETRYING, DEAD_LETTERED, WORKFLOW_COMPLETED events
  - React UI in `ui/`: 4 scenario buttons + scrolling color-coded filterable event log via EventSource SSE
  - Dev: `cd ui && npm run dev` (port 5173, proxies /events + /scenarios + /workflows to :8080)

## In Progress
Nothing.

## Deferred
- Phase 6: observability charts (in-flight count, retry rate, dead-letter count, per-step latency)
- Phase 7: README, architecture diagram, polish, v1 tag
- Dead-letter integration test
- Crash recovery — explicitly cut from v1

## Open Questions
None.

## Notes for Next Session
- Start Phase 6: observability panel
- Add second SSE channel or piggyback on existing one with different event types
- Charts: in-flight count over time, retry rate, dead-letter count, per-step latency histogram
- Lightweight charting lib (recharts or chart.js via CDN)
- Consider a 5s polling interval for aggregate metrics endpoint (GET /metrics/summary) rather than SSE for charts
- The "spike and recover" pattern should be visible: fire flaky-batch, watch retry rate spike, watch it decay as jobs complete
