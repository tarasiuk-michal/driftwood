# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Build & Run

```bash
# Start Postgres
docker compose up -d

# Build (skip tests)
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run tests (uses H2 in-memory, no Docker needed)
./mvnw test

# Run a single test class
./mvnw test -Dtest=WorkflowControllerTest

# Verify the running app
curl -s -X POST http://localhost:8080/workflows/trivial-workflow/instances | jq .
```

---

## Architecture

```
POST /workflows/{id}/instances
        │
        ▼
WorkflowController
        │
        ▼
OrchestratorService          ← @Transactional; single method owns the full lifecycle
        │
        ├── WorkflowRepository       → workflows table (Workflow + Steps)
        ├── WorkflowInstanceRepository → workflow_instances table
        └── StepExecutionRepository  → step_executions table
```

**Postgres is the source of truth** for all state. Kafka (added in Phase 1) is pure transport — dispatch and result messages only. Nothing important lives only in Kafka or in memory.

**Domain model:**
- `Workflow` + `Step` — definitions (seeded via Flyway, not created at runtime)
- `WorkflowInstance` + `StepExecution` — runtime state created per invocation

**Migrations:** Flyway in `src/main/resources/db/migration/`. V1 = schema, V2 = seed data. Always add new migrations as V3, V4, etc. — never modify existing ones.

**Tests:** `@SpringBootTest` with H2 (`MODE=PostgreSQL`) via `src/test/resources/application.yml`. Flyway runs the same migrations against H2; test methods annotated `@Transactional` roll back automatically.

**Phases:** See `DESIGN.md`. Check `PROGRESS.md` at session start. State current phase before writing any code.

---

# CLAUDE.md — Working Agreement for Driftwood

This file governs how Claude Code should work on this repository. It exists
for two reasons: to keep the project moving cleanly, and to be a readable
artifact in its own right — anyone looking at this repo should be able to
read this file and understand exactly how the human and the agent split
responsibility.

Read `DESIGN.md` before starting any work. It defines the phases; work
strictly in phase order unless told otherwise.

---

## Commit discipline

- **One logical change per commit.** If a single session produces several
  separable changes (e.g. "add retry poller" + "add dead-letter table"),
  propose splitting them into separate commits rather than bundling.
- **Never commit automatically.** After finishing a unit of work, stop and
  show me a `git diff --stat` summary and a short description of what
  changed and why. Wait for my go-ahead before running `git commit`.
- **Never write the commit message yourself.** Propose a one-line summary
  of *what* changed as a suggestion only, then explicitly ask me: "What
  commit message do you want for this?" Use whatever I give you verbatim.
  If I say "use your suggestion," that's fine — but the default is to ask,
  not assume.
- **Never push without asking**, even to a personal remote.
- **No commits that mix phases.** If you notice mid-task that something
  belongs to a later phase, stop and flag it rather than doing it early
  "while you're in there."

## Before writing code

- State which phase (from `DESIGN.md`) the current task belongs to.
- If a task seems to span multiple phases, say so and propose how to split
  it before writing anything.
- If a design decision has more than one reasonable approach (this will
  happen — retry strategy, idempotency storage, SSE vs. polling for the
  UI, etc.), present the options and tradeoffs and let me decide. Don't
  silently pick one. When I decide, remind me it's worth a short note in
  `docs/decisions/`.

## Testing

- New behavior (a new endpoint, a new state transition, a new failure
  mode) needs a test before the task is considered done. Don't mark
  something complete without one.
- Prefer a small number of meaningful tests (one happy path, one or two
  edge cases that matter — e.g. duplicate idempotency key, max-retries
  exceeded) over exhaustive coverage of trivial cases.

## Decision records

- When we make a non-obvious architectural call (retry mechanism,
  Postgres-vs-Kafka for some piece of state, SSE vs. WebSocket, etc.),
  create or update a file in `docs/decisions/ADR-NNN-short-title.md` with:
  what we chose, what we considered instead, and why. Keep each ADR under
  half a page.
- Propose the ADR at the time the decision is made, not retroactively at
  the end of a phase.

## Memory / continuity across sessions

This project is built across many separate Claude Code sessions. Use the
following to keep continuity without relying on chat history:

- **`PROGRESS.md`** (repo root) is the single source of truth for "where we
  are." At the end of every session, update it with: current phase, what's
  done, what's in progress, what's explicitly deferred, and any open
  questions for the human. Start every new session by reading this file
  first, before touching code.
- **`docs/decisions/`** is long-term memory for *why* — consult it before
  proposing something that contradicts a past decision. If you think a
  past decision should change, say so explicitly and explain why, rather
  than quietly overriding it.
- **Don't assume context from a previous session carries over** unless
  it's written down in `PROGRESS.md` or `docs/decisions/`. If something
  seems to be missing context, ask rather than guessing.

## Scope discipline

- `DESIGN.md` explicitly cuts crash recovery and a few other items from
  v1. Do not implement them "for completeness" — flag if you think
  something should be pulled into scope, but default to following the
  documented cut.
- If a task starts growing beyond what was asked, stop and check in rather
  than continuing to expand it.

## Tone

- Be direct about tradeoffs and risks, including in code review of your
  own output. If something is a hack, fragile, or you're not confident in
  it, say so plainly rather than presenting it as finished.
- Disagree if you think a requested approach has a real problem. Don't
  silently comply with something you'd flag if asked directly.
