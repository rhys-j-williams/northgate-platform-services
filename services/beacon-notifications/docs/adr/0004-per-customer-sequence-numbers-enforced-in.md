# ADR 0004: Per-customer sequence numbers enforced in the consumer, not in MQ

Status: Accepted
Owner: payments-platform

## Context

INC0084410: two balance alerts arrived out of order because two consumer threads picked
consecutive messages. MQ preserves order per queue, not per consumer pool, and the event producer
cannot be changed (it is Bedrock).

## Decision

Every event carries a per-customer `sequenceNumber` assigned by the producer-side adapter. Beacon
keeps a `SequenceCoordinator` (in Oracle, `BEACON_SEQUENCE`) with the last dispatched sequence per
customer. A message ahead of its turn is parked for up to `gap-timeout` then dispatched with a
`gapDetected` flag. Single consumer thread per customer partition is not possible with the MQ
client we have, so this is done in application code.

## Consequences

* Ordering is correct under normal operation and degrades to "eventually, flagged" under gaps.
* The gate is a hot path with a database write per event. Oracle copes; H2 in tests copes.
* Testing this properly needs a real broker and multiple consumers, which the CI agents do not give us.
