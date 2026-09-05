# ADR 0001: Read-through archive: render on first request, serve from the store thereafter

Status: Accepted
Owner: retail-digital

## Context

Statement rendering is slow (ReportLab, ~300ms) and the previous vendor pre-rendered every
statement monthly whether or not anyone looked. 80% were never opened.

## Decision

documents-service owns the archive. First request for an account/period streams from
statements-api and tees to the store. Subsequent requests come from the store. Periods older than
7 years are not rendered on demand (compliance retention boundary) and return 410.

## Consequences

* Storage grows with what customers actually read.
* The first open is slower. Acceptable.
* Streams are teed, not buffered, so a 50 page statement does not sit in Node memory.
