# entitlements-service

Java 17, Spring Boot 3.1.12 | port 4515 | version 4.1.0 | owner **identity-platform** (g.mwangi, o.lindqvist)

Roles, entitlements and dual-approval policy for the business channel. Which operator may do
what on which organisation, and which actions need a second pair of eyes at what threshold.
bff-business asks; this service answers. Fixture organisations come from `@meridian/domain-fixtures`
via the shared JSON export.

This is the one backend already on Java 17 and Spring Boot 3.1.12, done in 2024 as the pilot for
the platform's Boot 3 migration (PLAT-1352). The pilot report is in `docs/adr/0002`. Note the
`jakarta.*` imports and the Log4j2 configuration that is duplicated from common-starter because
the starter is still on `javax` (see BUILD note in the README of `libs/java/common-starter`).

## On call

identity-platform, Chester (08:00-18:00 UK). US afternoon covered by payments-platform Jersey City under the shared services agreement. This is the only Boot 3 service and the on-call notes assume you know that.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/entitlements/v1/roles` | Role catalogue. |
| `GET` | `/entitlements/v1/organisations` | Fixture organisations with their operators and roles. |
| `POST` | `/entitlements/v1/check` | `{"operatorId","organisationId","action"}` -> `{"allowed", "requiresApproval", "reason"}`. |
| `GET` | `/entitlements/v1/approvals` | Pending dual-approval items. Requires `entitlements:approvals`. |
| `POST` | `/entitlements/v1/approvals/{id}/decision` | Maker cannot be checker. 409 `SAME_ACTOR`. |
| `GET` | `/actuator/health` |  |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4515**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 55%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/entitlements-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1352** Boot 3 pilot. `common-starter` is not consumed here; the correlation filter, error advice and JSON logging are copied under `com.meridian.entitlements.platform`. Drift is inevitable and has already happened once (the error envelope gained `violations` in the starter and not here for two months).
- **KEY-1620** Role catalogue is Flyway-seeded, not synced from Keystone groups. Keystone are meant to publish a `roles.changed` topic. They have not.
- **PLAT-1701** Approval thresholds are in `application.yml`. Business want them per organisation. Data model supports it, API does not.

## History

Extracted from bff-business in 2024 as part of the dual approval redesign (PLAT-1352, LDG-0912)
and used as the Boot 3 pilot at the same time, which in hindsight was two changes in one. The
pilot went to prod in June 2024 without incident, and the platform Boot 3 programme has been
"next quarter" since.

See `docs/adr/` and `docs/runbooks/`.
