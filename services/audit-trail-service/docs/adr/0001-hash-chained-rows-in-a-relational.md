# ADR 0001: Hash-chained rows in a relational store rather than a WORM appliance

Status: Accepted
Owner: platform-engineering

## Context

SOX control CTL-AUD-004 requires tamper evidence on the audit trail. Procurement for a WORM
appliance was a 9 month process.

## Decision

Each row stores the hash of its predecessor. DB2 table has INSERT-only grants for the service
account; UPDATE/DELETE are revoked at the database. Verification is a service endpoint and a
monthly batch job whose output goes to the control evidence folder.

## Consequences

* Tamper evident, not tamper proof. A DBA with the right grants can rewrite the chain. Compensating control is DBA activity monitoring, out of our scope.
* Timestamp precision issues across databases (PLAT-1466).
