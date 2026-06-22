# Driftwood — Design Doc

A Kafka-backed workflow orchestrator with retries, idempotency, and dead-letter
handling, paired with a live "control room" UI that fires chaos scenarios at
it and shows the system recover in real time.

Jobs that get knocked off course (failures, retries, crashes) and still
reliably make it to shore.

---

## Architecture Overview

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
                                          │   Worker(s)  │
                                          │ (simulated   │
                                          │  step exec)  │
                                          └──────────────┘
```

**Core principle for the whole build:** Postgres is the source of truth for
all state (workflow/step status, retry schedule). Kafka is pure transport —
dispatch and result messages. Nothing important lives only in Kafka or only
in memory. This is what makes retries, dead-lettering, and (later) crash
recovery clean to reason about and to explain.

---

## Phases

Each phase is its own set of commits, individually demoable, individually
"complete." Don't start phase N+1 until phase N runs end-to-end.

### Phase 0 — Scaffolding
- Spring Boot project (Java 21), Postgres via Docker Compose, Flyway for
  schema migrations
- Domain model, no Kafka yet: `Workflow` (definition), `WorkflowInstance`,
  `Step` (definition), `StepExecution`
- Bare REST endpoint: `POST /workflows/{id}/instances` → creates instance,
  runs synchronously, returns result
- One trivial workflow definition (e.g. 2 steps, both just sleep + succeed)
  to prove the model
- **Done when:** you can curl a workflow into existence and watch it
  complete, state is in Postgres, no async yet

### Phase 1 — Kafka transport, single step, happy path
- Add Kafka (docker-compose), topics: `driftwood.step.dispatch`,
  `driftwood.step.result`
- Orchestrator publishes dispatch on instance creation instead of running
  inline
- A worker (separate Spring component or service) consumes dispatch,
  "executes" (simulated — sleep + succeed/fail based on config), publishes
  result
- Orchestrator consumes result, updates Postgres state
- **Done when:** submitting a job is fully async, end-to-end through Kafka,
  state still correctly lands in Postgres

### Phase 2 — Retry with backoff (the centerpiece design decision)
- On step failure: don't republish to Kafka immediately. Write
  `next_retry_at` + incremented `attempt_count` to Postgres, mark instance
  `RETRYING`
- A scheduled poller (e.g. every 1s) scans for rows where
  `next_retry_at <= now()` and republishes to dispatch topic
- Exponential backoff calculation (attempt → delay), capped attempt count
- After max attempts: mark `DEAD_LETTERED`, write to a dead-letter table
  (and optionally a Kafka DLQ topic for symmetry, but Postgres is the real
  record)
- **Done when:** you can submit a job configured to fail twice then succeed,
  and watch it actually retry on schedule and complete; submit one that
  always fails and watch it dead-letter
- **Why Postgres-polling retries instead of Kafka delay-topics:** retrying
  via Kafka means either blocking the consumer thread (which stalls every
  other message on that partition behind the slow retry) or building a
  tiered delay-topic system. Polling Postgres reuses the state that's
  already the source of truth, keeps the dispatch path non-blocking, and
  keeps retry timing exact instead of approximate. Tradeoff: introduces a
  polling component and a latency floor tied to poll interval. Worth a short
  ADR (see `docs/decisions/`).

### Phase 3 — Idempotency
- Client supplies an idempotency key on submission; duplicate key within a
  time window returns the existing instance instead of creating a new one
- Worker-side idempotency: if a `StepExecution` for a given
  (instance, step, attempt) already has a terminal result recorded, a
  duplicate dispatch message is a no-op, not a re-execution
- **Done when:** firing the same submission twice produces one instance;
  replaying a dispatch message manually doesn't double-execute

### Phase 4 — Multi-step workflows
- Extend `Workflow` definition to an ordered list of steps
- Orchestrator advances to next step only after current step's result is
  `SUCCESS`; a step `FAILURE` triggers that step's own retry cycle, not the
  whole workflow's
- **Done when:** a 3-step workflow where step 2 fails twice then succeeds
  completes correctly, with steps 1 and 3 each running exactly once

### Phase 5 — Control Room UI, part 1: scenario launcher + live log
- React app, single page
- **Scenario launcher:** 3–4 named buttons, e.g. "Submit clean batch
  (20 jobs)," "Submit flaky batch (30% step-2 failure)," "Submit poison job
  (always fails)," "Submit duplicate (idempotency test)" — each just calls a
  backend endpoint that creates pre-configured jobs
- **Live event log:** SSE stream from backend pushing state transitions
  (submitted/dispatched/retrying/succeeded/dead-lettered) as they happen,
  rendered as a scrolling, color-coded, filterable list
- **Done when:** clicking a scenario button visibly populates the live log
  in real time with a believable, varied stream of events

### Phase 6 — Control Room UI, part 2: observability panel
- Live-updating charts (in-flight count, retry rate over time, dead-letter
  count, per-step latency) — a lightweight charting lib, data pushed over
  the same SSE channel or a second one
- This is the visual payoff — the panel should make system stress and
  recovery visibly readable: spike on chaos scenario, decay back to
  baseline as the system catches up
- **Done when:** firing the flaky-batch scenario produces a visible,
  legible spike-and-recover pattern on the charts without you narrating it

### Phase 7 — Polish & sanitize for publishing
- README: problem statement, architecture diagram, the backoff
  design-decision writeup, how to run (`docker-compose up`),
  screenshots/GIF
- Strip anything Arval/SII-specific from commit messages or comments
- Tag a `v1` release

**Deliberately cut from v1, stated as such in the README:** crash recovery
(orchestrator restart mid-flight) — named as a forward-looking item with a
one-paragraph sketch of how you'd approach it, not built. This is a
legitimate scoping decision, not a hidden gap, because it's stated
explicitly.

---

## On clean history + visible AI usage

- **Commit per logical step, not per Claude Code session.** If Claude Code
  does five small things in one session, that can be five commits if
  they're genuinely separable. Resist one giant "phase 2" commit — granular
  history is more credible and more reviewable.
- **Write your own commit messages**, even when the diff was agent-assisted
  — this is your decision record, and "what" + "why" in your own words is
  what a reviewer actually wants, not a generated changelog line.
- **Keep `CLAUDE.md` in the repo from commit one**, evolving as you go —
  this is itself evidence, and a recruiter or engineer who opens it sees
  your actual working conventions, not a polished-after-the-fact document.
- **Don't squash history before publishing.** A real, slightly messy commit
  log with visible iteration (including a revert or a "fix retry calc
  off-by-one" commit) is more convincing than a single pristine commit — it
  looks like real work, not a staged showcase.
- **Keep a `docs/decisions/` folder** with short ADR-style notes (e.g.
  "ADR-001: Postgres-polling retries over Kafka delay-topics") written as
  you make the call, not retrofitted at the end. This is the highest-value
  artifact for the "I understand the tradeoffs" story and takes about
  15 minutes per decision.
