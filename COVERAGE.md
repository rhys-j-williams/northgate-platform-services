# Coverage

Generated 2026-09-05 by `make coverage` (`scripts/coverage-md.py`). Line coverage, as reported by the tools;
the Sonar quality gate reads the same files. Numbers here are the numbers, do not round them up in
slide decks.

| Service | Stack | Target | Actual | Lines | Source | Note |
|---|---|---|---|---|---|---|
| `bff-retail` | Node 18 / NestJS 9 | 35% | **36.5%** | 605 | Jest line (branches 34.8%) |  |
| `bff-business` | Node 18 / NestJS 9 | 30% | **34.6%** | 482 | Jest line (branches 25.8%) |  |
| `beacon-notifications` | Java 11 / Boot 2.7.18 | 25% | **25.4%** | 625 | JaCoCo line | no tests on `SequenceCoordinator` (PLAT-1288) |
| `alerts-preferences-service` | Java 11 / Boot 2.7.18 | 40% | **41.3%** | 312 | JaCoCo line |  |
| `txn-posting-service` | Java 11 / Boot 2.7.18 | 15% | **17.5%** | 292 | JaCoCo line | no reversal or idempotency edge tests (PLAT-1201) |
| `pii-vault-service` | Java 11 / Boot 2.7.18 | 8% | **8.5%** | 295 | JaCoCo line | FPE known-answer test only (RA-2022-0341) |
| `audit-trail-service` | Java 11 / Boot 2.7.18 | 12% | **16.5%** | 170 | JaCoCo line | Kafka consumer untested (PLAT-1289) |
| `entitlements-service` | Java 17 / Boot 3.1.12 | 55% | **59.2%** | 409 | JaCoCo line |  |
| `bedrock-adapter` | Java 11 / Boot 2.7.18 | 20% | **24.5%** | 682 | JaCoCo line | codec covered, MQ paths not |
| `iris-orchestrator` | Node 18 / NestJS 9 | 40% | **45.9%** | 497 | Jest line (branches 36.2%) |  |
| `documents-service` | Node 18 / Express | 25% | **26.0%** | 300 | Jest line (branches 21.1%) | statements/tax routes via smoke only (PLAT-1877) |
| `statements-api` | Python 3.11 / FastAPI | - | - | - | none | no test framework, no tests, no CI test stage (CAB-2021-1188) |
| `exposure-calc` | Python 3.11 / FastAPI | - | - | - | none | no tests, no infrastructure |

**Overall (line-weighted across the 11 measured services): 32.0%.**
JaCoCo aggregate across the seven Java services: **28.8%** over 2785 lines
(`build/coverage-aggregate/target/site/jacoco-aggregate/index.html`).

## Reading this table

The compliance critical services (txn-posting, pii-vault, audit-trail) are the worst covered. That is
not an accident of history so much as a consequence of it: they were written under deadline, their
owners changed, and each has a risk acceptance or CAB exception standing in for the tests. The
exceptions are listed in each service README under Known Issues. GIS have asked for a remediation
plan (GIS-2201); there is a plan, it is a Confluence page, and it has no dates on it.

The two Python services report nothing because there is nothing to report. statements-api is on the
documents-service smoke path and is the one people worry about.

Targets are the `coverageThreshold` in each Jenkinsfile and the matching `sonar-project.properties`.
A service under its target fails its pipeline; when that happens the fix has historically been to
lower the target (see git log for txn-posting-service, 2022) rather than to write the tests.
