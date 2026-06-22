# ADR-001: Postgres-polling retries over Kafka delay-topics

**Status:** Accepted  
**Phase:** 2

## Decision

On step failure, write `next_retry_at` and increment `attempt_count` in Postgres,
then mark the instance `RETRYING`. A scheduled poller (every 1s) scans for rows
where `next_retry_at <= now()` and re-dispatches to the Kafka dispatch topic.

## Alternatives considered

**Kafka delay-topics (tiered retry pattern):** Create separate topics per retry tier
(e.g. `retry-5s`, `retry-30s`, `retry-5m`). Consumer publishes to the appropriate
tier on failure; a router consumer reads each tier and re-dispatches when the delay
has elapsed.

## Why Postgres polling

- Retrying via Kafka means either sleeping the consumer thread (stalls every other
  message behind it on that partition) or maintaining a tiered delay-topic system
  with a router consumer per tier.
- Polling Postgres reuses the state that is already the source of truth. Retry
  schedule, attempt count, and status are already there — no new system to reason
  about.
- Retry timing is exact to the poll interval (1s), not approximate.
- Dead-lettering is a Postgres row update, not a DLQ topic that could diverge.

## Tradeoffs accepted

- Introduces a polling component (`RetryPoller`) and a minimum latency floor
  tied to poll interval (1s). Fine for this use case.
- At high volume, the poller query scans `step_executions` on `next_retry_at`;
  needs an index on that column (added in the migration).
