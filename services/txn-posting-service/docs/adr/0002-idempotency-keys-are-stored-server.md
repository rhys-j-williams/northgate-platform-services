# ADR 0002: Idempotency keys are stored server side with the request hash

Status: Accepted
Owner: payments-platform

## Context

INC0086001. Clients retry. Bedrock has no idempotency on TXNPOST.

## Decision

`Idempotency-Key` is mandatory. Store key, SHA-256 of the canonical request body, and the response
for 72h in `POSTING_IDEMPOTENCY`. Same key and hash replays the stored response. Same key,
different hash is rejected. The insert is the first thing in the transaction so a concurrent
duplicate hits the unique constraint and waits.

## Consequences

* Correct under the cases we thought of. The cases we did not think of are PLAT-1201.
* Oracle row per posting for 72h. Purge job is the weak point (PLAT-1580).
