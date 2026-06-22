# PROGRESS.md

## Current Phase
**Phase 0 — Scaffolding** ✅ Complete

## Done
- Spring Boot 3.3.5 / Java 21 project (`pom.xml`)
- Postgres 16 via Docker Compose, Flyway migrations (V1 schema, V2 seed)
- Domain model: `Workflow`, `Step`, `WorkflowInstance`, `StepExecution` with status enums
- REST endpoint: `POST /workflows/{id}/instances` — creates instance, executes steps synchronously, returns result
- Trivial workflow seeded: `trivial-workflow` with 2 steps (Prepare → Execute)
- Integration test: happy path + 404 for unknown workflow
- H2 used for tests (PostgreSQL compatibility mode); real Postgres via Docker Compose for dev

## In Progress
Nothing.

## Deferred
- Kafka transport (Phase 1)
- Retry with exponential backoff + dead-letter (Phase 2)
- Idempotency keys (Phase 3)
- Multi-step advancement logic with per-step retry cycle (Phase 4)
- Control Room UI — scenario launcher + live SSE log (Phase 5)
- Control Room UI — observability charts (Phase 6)
- Polish, README, v1 tag (Phase 7)
- Crash recovery — explicitly cut from v1 (see DESIGN.md)

## Open Questions
None.

## Notes for Next Session
- Start Phase 1: add Kafka to `docker-compose.yml`, create `driftwood.step.dispatch` and `driftwood.step.result` topics
- Key decision for Phase 1: worker as separate Spring `@Component` within same app vs. a second Spring Boot module — present options before writing
- ADR-001 (Postgres-polling retries) should be written at the start of Phase 2 before any code
