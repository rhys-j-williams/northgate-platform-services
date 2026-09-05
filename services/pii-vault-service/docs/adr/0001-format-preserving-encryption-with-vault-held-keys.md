# ADR 0001: Format-preserving encryption with Vault-held keys instead of a token vault table

Status: Accepted
Owner: payments-platform

## Context

PCI scope reduction needed PANs out of the analytics zone. A lookup-table token vault would itself
be in scope and be a single point of failure for every batch.

## Decision

FF1-style format-preserving encryption, keys in Vault transit, no token table. Tokens are
deterministic per key version so joins still work downstream. Access is logged, not gated by a
table.

## Consequences

* No token table to protect, no lookup latency.
* Deterministic tokens are linkable. Accepted for the internal analytics use case; not acceptable for anything customer facing, hence the scope check on `detokenise`.
* Key compromise means re-tokenising everything. Rotation procedure exists on paper (PLAT-1155).
