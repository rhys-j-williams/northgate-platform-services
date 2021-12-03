# ADR 0001: Render statements on demand in Python with ReportLab

Status: Accepted
Owner: payments-platform

## Context

Paperless statements pilot, six weeks, no Java capacity on the team. ReportLab was already
approved for the treasury reporting scripts.

## Decision

Small FastAPI service, ReportLab Platypus, render on demand, cache nothing (documents-service
caches). Ship it as the spike; replace it after the pilot.

## Consequences

* Shipped on time. Never replaced.
* No tests, because the spike had none and adding them to a service "about to be replaced" never made the sprint.
* Python is otherwise absent from platform-services and the Jenkins library has no Python pipeline; the Jenkinsfile here is a hand written scripted pipeline that the platform team tolerate.
