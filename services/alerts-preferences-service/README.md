# alerts-preferences-service

Java 11, Spring Boot 2.7.18 | port 4511 | version 2.9.1 | owner **payments-platform** (a.balaraman, p.venkatesan)

Owns the customer's alert and channel preferences and the rule that **regulatory alerts cannot
be disabled**. Beacon reads from here; retail-web writes to here through bff-retail. Every
change is published to Kafka (`alerts.preferences.changed.v1`) for Beacon's cache and for
audit-trail-service. When Kafka is unavailable the change is still committed to Oracle and the
event is spooled to `ALERT_PREF_OUTBOX`; a scheduled publisher drains it.

## On call

PLAT-BEACON rota (shared with Beacon). Business hours only for non-regulatory issues.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/alerts/v1/customers/{customerId}/preferences` | Effective preferences, with `locked: true` on regulatory codes. |
| `PUT` | `/alerts/v1/customers/{customerId}/preferences/{alertCode}` | 409 `REGULATORY_LOCK` when trying to disable a regulatory alert or remove its last channel. |
| `GET` | `/alerts/v1/catalogue` | Alert codes and their regulatory flag. The flag comes from `ALERT_CATALOGUE`, seeded by Flyway. |
| `GET` | `/actuator/health` |  |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4511**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 40%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/alerts-preferences-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1377** Outbox drain is single threaded and runs every 10s. A long Kafka outage builds a backlog that takes minutes to clear after recovery.
- **PLAT-1466** The regulatory flag is per alert code, not per jurisdiction. State-specific disclosures are handled in Beacon templates instead. This is wrong and everyone agrees it is wrong.
- **MOL-2098** Preferences page in retail-web shows the SMS channel for customers with no mobile number on file; we return it because the fixture customers all have one.

## History

Split out of Beacon in 2022 (PLAT-1041) because the preferences write path kept getting deployed
alongside dispatch changes and Compliance wanted them separately change-controlled. The
regulatory lock was the first thing written and the best tested thing in the service.

See `docs/adr/` and `docs/runbooks/`.
