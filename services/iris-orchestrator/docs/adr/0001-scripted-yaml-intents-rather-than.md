# ADR 0001: Scripted YAML intents rather than an NLU vendor

Status: Accepted
Owner: retail-digital

## Context

The contact centre wanted a bot in a quarter. The NLU vendor evaluation (three products) needed
data sharing agreements that Legal estimated at six months.

## Decision

Keyword-and-regex intents in YAML, owned by the content team through pull requests, reviewed by
retail-digital. Authenticated intents proxy to bff-retail rather than reaching Bedrock directly.
Anything not matched twice in a row goes to a human.

## Consequences

* Cheap, predictable, auditable. Compliance like that every possible reply is in a file.
* It is not clever and it never will be. The product team know.
* YAML changes are deployments (post INC0097712).
