# ADR 0001: Transactional outbox for preference change events

Status: Accepted
Owner: payments-platform

## Context

Beacon and audit must see every preference change. Dual writes (Oracle then Kafka) lost events
during the Sept 2022 Kafka upgrade.

## Decision

Write the event to `ALERT_PREF_OUTBOX` in the same transaction as the preference row. A scheduled
publisher sends and deletes. Consumers must tolerate duplicates (the event carries the preference
version).

## Consequences

* At-least-once delivery, ordered per customer by version.
* Extra table, extra scheduler, extra thing to monitor (`alert_pref_outbox_depth`).
