# Driftwood v1 — Demo Script

Run in order; each step should produce a specific, checkable result. Don't
skip ahead if one fails.

Quick gut-check before running this: v1 took one evening with Claude Code.
That's fast for retries + idempotency + dead-letter + multi-step + control
room UI + observability. Worth being honest with yourself about whether the
hard paths were actually exercised (concurrent retries under load, the
poller racing with new dispatches, the UI under a real flaky-batch stress
run) or whether Claude Code mostly got the happy paths working end-to-end.
This script is partly designed to answer that — if any step surprises you,
that's useful signal before showing this to anyone.

---

## Setup

1. `docker-compose up` — Postgres + Kafka + app + UI all healthy
2. Open control room UI, confirm live log panel connects (should show a
   "connected" state or heartbeat, not just silence)
3. Confirm observability panel renders with zero/baseline values before
   anything is submitted

## 1. Happy path

- Fire "submit clean batch (20 jobs)"
- **Expect:** 20 entries appear in the live log, all transition
  submitted → dispatched → succeeded, no retries, observability shows a
  brief blip in in-flight count returning to zero
- **Check:** do all 20 actually reach `succeeded` in Postgres?
  ```sql
  SELECT status, count(*) FROM workflow_instance GROUP BY status;
  ```

## 2. Retry + recovery

- Fire "submit flaky batch (30% step-2 failure)"
- **Expect:** some jobs retry visibly (state goes to `RETRYING`, log shows
  backoff delay, then re-dispatch), retry-rate chart spikes then decays,
  all jobs eventually reach `succeeded` (none should dead-letter at 30%
  failure with reasonable max-attempts)
- **Check:** pick one job from the log that retried — does its attempt
  count in Postgres match what the UI showed? Does the delay between
  attempts roughly match your configured backoff (not instant, not stuck)?

## 3. Dead-letter

- Fire "submit poison job (always fails)"
- **Expect:** visible retries up to max attempts, then transition to
  `DEAD_LETTERED`, appears in dead-letter count on observability panel
- **Check:** query the dead-letter table directly — is the failure
  reason/error recorded, or just a status flag? (If just a flag, that's a
  real v1 gap worth fixing before showing anyone — "why did it fail" is
  the first question an interviewer asks.)

## 4. Idempotency

- Fire "submit duplicate (idempotency test)" twice in a row with the same
  key (or fire it once via the button, then manually `curl` the same
  idempotency key again)
- **Expect:** second submission returns the *existing* instance, no
  second row created
- **Check:**
  ```sql
  SELECT count(*) FROM workflow_instance WHERE idempotency_key = '...';
  ```
  Must be 1, not 2.

## 5. Concurrency / load (the one most likely to surprise you)

- Fire clean batch + flaky batch + poison job **simultaneously**, back to
  back, don't wait between clicks
- **Expect:** log stays readable (filterable, not a wall of noise),
  observability charts show overlapping activity without the UI freezing
  or the SSE stream dropping
- **Check:** does anything get stuck in `RETRYING` forever? Does the
  poller keep up, or does a backlog build? This is the scenario most
  likely to expose a race you haven't hit yet — worth running 2–3 times.

## 6. Cold restart (not crash recovery — just sanity)

- Submit a flaky batch, then restart the app container mid-retry-cycle
  (`docker-compose restart app`), without touching Postgres/Kafka
- **Expect** (since crash recovery wasn't built in v1): some jobs likely
  get stuck or behave oddly — that's fine and expected. The point is to
  **know exactly what breaks** so you can say it precisely in an
  interview, rather than "I didn't test that."

---

## What to do with the results

- If steps 1–4 are solid and 5 surprises you: fix what you find. That's
  normal and exactly the kind of bug worth having a story about ("found a
  race in the poller under concurrent load, here's the fix").
- If 1–4 have problems: that's worth more build time before touching v2.
