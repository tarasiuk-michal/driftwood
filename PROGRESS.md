# PROGRESS.md

## Current Phase
**Phase 7 — Polish & publish** ✅ Complete — v1 ready

## Done
- **Phase 0:** Spring Boot 3.3.5 / Java 21, Postgres + Flyway, domain model, synchronous REST endpoint, trivial workflow seed, integration test
- **Phase 1:** Kafka (Bitnami KRaft), async dispatch/result, WorkerService, OrchestratorService, GET status endpoint, EmbeddedKafka tests
- **Phase 2:** Postgres-polling retry with exponential backoff, dead-letter table, RetryPoller, WorkerProperties, ADR-001
- **Phase 3:** Idempotency-Key header, idempotency_keys table, worker-side duplicate result no-op
- **Phase 4:** three-step-workflow seed, MultiStepWorkflowTest, fixed cross-context H2 isolation
- **Phase 5:** WorkflowEventBus (SSE), SseController, ScenarioController, React UI — scenario buttons + live event log
- **Phase 6:** MetricsController (/metrics/summary), MetricsPanel with SVG sparklines (in-flight / retrying / completed + dead-letter count + avg latency)
- **Phase 7:** README.md with problem statement, architecture diagram, retry design write-up, how-to-run, API table, scope cuts

## In Progress
Nothing.

## Deferred
- Crash recovery — explicitly documented in README as out-of-scope forward item
- GIF/screenshot in README (nice-to-have for publish)

## Open Questions
None — project is feature-complete per DESIGN.md.

## Next Steps
- Push to remote (all phases, or squash per-phase commits)
- Tag v1 release: `git tag v1.0.0 && git push origin v1.0.0`
- Optional: add GIF of control room in README before publish
