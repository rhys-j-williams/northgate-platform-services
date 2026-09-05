# ADR 0003: One BFF per channel rather than a shared API gateway

Status: Accepted
Owner: retail-digital

## Context

Retail and business front ends need different shapes of the same data, different entitlement
models and different SLAs. The API gateway team proposed a single graph.

## Decision

Separate BFFs owned by the channel teams (`bff-retail` by retail-digital, `bff-business` by
business-digital). Shared concerns (JWT validation, correlation, error envelope) live in copied
`common/` modules rather than a shared package because the two teams release on different trains
and a shared package broke both of them in Feb 2022 (MOL-1502).

## Consequences

* Duplicated code in `src/auth` and `src/common` across the BFFs. Known, accepted, revisited annually and never changed.
* Each BFF can degrade independently.
* Iris talks to bff-retail for balances rather than Bedrock directly, so bff-retail is on the Iris critical path too.
