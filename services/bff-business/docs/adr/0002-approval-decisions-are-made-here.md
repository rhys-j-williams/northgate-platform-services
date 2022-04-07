# ADR 0002: Approval decisions are made here, dual-approval rules are owned by entitlements-service

Status: Accepted
Owner: business-digital

## Context

Ledgerline needed maker-checker on payments. The rules (who may approve what, thresholds) belong
with roles; the queue belongs with the channel.

## Decision

bff-business owns the approvals queue (Redis list, in-memory fallback) and the user-facing API.
entitlements-service owns the policy and answers `can operator X approve item Y`. bff-business
never encodes a threshold.

## Consequences

* Two network hops for every approval decision. Fine.
* The legacy `ApprovalPolicy` class stays until LDG-0912 lands.
* Losing the queue on restart in fallback mode is a real risk we have chosen to live with in dev/uat only.
