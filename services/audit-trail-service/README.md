# audit-trail-service

Java 11, Spring Boot 2.7.18 | port 4514 | version 2.3.4 | owner **platform-engineering** (e.castellanos, a.balaraman)

**Compliance critical.** SOX/PCI in scope. Changes need a CAB reference and a GIS AppSec reviewer (CODEOWNERS enforces the second).

Compliance critical. Append-only audit log in DB2 (H2 in DB2 mode locally). Consumes
`audit.events.v1` off Kafka and also accepts direct POSTs from services that predate the topic.
Every row carries the SHA-256 of the previous row (`PREV_HASH`) so the chain can be verified;
`GET /audit/v1/verify` walks it. Kafka offsets are recorded per partition and a re-delivered
offset is dropped, which is what makes the consumer idempotent.

Starts without Kafka: the consumer retries in the background and the REST path works. That is
deliberate (the audit store must accept writes even when the bus is down) and it is tested,
which is about the only thing here that is.

## On call

PLAT-AUDIT, business hours. Audit is not customer facing; a backlog on the Kafka topic is tolerable for hours, a hash chain break is not.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/audit/v1/events` | Direct append. `source`, `actor`, `action`, `subject`, `payload` (JSON, stored as CLOB). |
| `GET` | `/audit/v1/events` | Query by `actor`, `subject`, `from`, `to`. Paged. No free text search, use the Splunk feed. |
| `GET` | `/audit/v1/verify` | Walks the hash chain. Returns the first broken row if any. Slow on large tables; there is a `from` parameter. |
| `GET` | `/actuator/health` | Kafka consumer state under `components.kafka`; `DEGRADED` when not connected, service still UP. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4514**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 12%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/audit-trail-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1466** Hash covers the row fields truncated to millisecond timestamps because DB2 and H2 round differently. Discovered the hard way in the 2023 DB2 11.5 upgrade (INC0102201).
- **PLAT-1520** `verify` on the prod table takes 40 minutes. Run it from the batch schedule, not from a browser.
- **PLAT-1289** No tests on the Kafka consumer at all. Embedded Kafka was tried and removed because it added 90s to the build. Offset dedup is tested via the repository, not via the consumer.
- **TOOL-1201** DB2 driver licence jar is not in Artifactory. The prod profile is commented out for this reason as much as for the datasource.

## History

The audit service is the oldest code in platform-services by lineage: it started as a JAX-RS
app on WebSphere in 2019, was ported to Boot 2.2 in 2020 and has been on the 2.7 line since the
2023 bump train. DB2 because the original data owner was the audit function and their DBAs run
DB2. The hash chain was added in 2021 for the SOX control redesign (CTL-AUD-004).

See `docs/adr/` and `docs/runbooks/`.
