# Driftwood v2 — Design

Features deliberately cut from v1, planned properly this time. Recommend
tackling **crash recovery first** — it's the deferred item with the highest
demo value, and combined with the idempotency work already in v1, it
completes the "this is a properly engineered distributed system" claim.

---

## Crash Recovery

The deferred centerpiece. On orchestrator startup:

- Query Postgres for any `WorkflowInstance` in a non-terminal state
  (`RUNNING`, `RETRYING`) with no matching in-flight dispatch acknowledged
  recently
- Re-derive what should happen next purely from Postgres state (current
  step, attempt count, `next_retry_at`) — never trust in-memory state
  across a restart
- Re-publish dispatch for anything that should be in-flight but isn't

**Demo angle:** a "kill orchestrator mid-batch" button in the control room
UI, then watch it come back up and visibly resume exactly where it left
off. This is the best single demo moment — more impressive live than
anything in v1.

**Hard edge case to handle deliberately:** a step that *was* dispatched
right before the crash — did the worker actually execute it or not?
Without care you either skip a step (worker did finish, orchestrator
doesn't know) or double-execute (worker didn't finish, gets redispatched,
but had partially run). This is exactly what the v1 idempotency work
should make safe — worth explicitly testing.

---

## Observability — depth pass

- Per-workflow-type breakdown (not just global totals) — useful once
  there's more than one workflow definition
- Latency percentiles (p50/p95/p99) per step, not just averages
- A simple "replay last N minutes" view — useful for showing what
  happened during a demo after the fact, without needing to have watched
  it live

---

## Backpressure / load shedding

- What happens if 10,000 jobs get submitted at once? Right now: probably
  the poller falls behind silently. Add a visible queue-depth metric and a
  deliberate (even simple) shedding or throttling policy once depth
  crosses a threshold
- **Why this matters for the story:** "what happens under real load" is a
  natural senior-level follow-up question; having an actual answer (even a
  simple one) beats not having tested it

---

## Workflow branching

- Currently steps are a strict linear sequence. Add conditional branching
  (step 3 depends on step 2's *result value*, not just success/failure) —
  turns this from a job queue into something closer to a real workflow
  engine, and is a natural "what would you add next" answer if asked in an
  interview before it's built

---

## Admin / operability

- Manual requeue / cancel / force-dead-letter actions on a specific
  instance, exposed in the UI — turns the control room from a demo toy
  into something that looks like a real operational tool
- Structured audit log per instance (full timeline of every state
  transition with timestamps) — also doubles as the most convincing
  "replay this for an interviewer" artifact

---

## Lower priority / stretch

- Multi-worker scaling demo (run 3 workers, show load distributed across
  them)
- Pluggable step types (currently simulated; a real step type — e.g. an
  HTTP call to a flaky test endpoint — would make the chaos feel less
  synthetic)
