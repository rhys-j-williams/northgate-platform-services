# txn-posting-service

Java 11, Spring Boot 2.7.18 | port 4512 | version 4.1.7 | owner **payments-platform** (j.hollins, a.balaraman)

**Compliance critical.** SOX/PCI in scope. Changes need a CAB reference and a GIS AppSec reviewer (CODEOWNERS enforces the second).

Compliance critical. Validates a posting request (account exists, not frozen, sufficient funds
where the product requires it, daily limits), posts to Bedrock via bedrock-adapter, records the
outcome in Oracle, and supports reversals. Idempotency is by `Idempotency-Key` header, stored
with the request hash for 72h: same key + same hash returns the original result; same key +
different hash is 422 `IDEMPOTENCY_CONFLICT`.

There are no tests on the reversal path and no tests on idempotency edges (key reuse after
expiry, concurrent first requests with the same key). PLAT-1201 has been open since 2022 asking
for them. The reversal path is exercised in UAT by the payments regression pack, which is a
spreadsheet and a person.

## On call

PLAT-POSTING. 24x7, primary in Jersey City, secondary Chennai. This is a SOX in-scope service; changes need a CAB reference.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/postings/v1` | Requires `Idempotency-Key`. 201 with the Bedrock reference, 202 when Bedrock accepted asynchronously (rare, only on Artemis). |
| `POST` | `/postings/v1/{postingId}/reversal` | Full reversal only. Partial reversals are a different Bedrock transaction we do not call. |
| `GET` | `/postings/v1/{postingId}` |  |
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

Runs on **4512**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 15%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/txn-posting-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1201** No automated tests for reversals or idempotency edges. See above. Coverage is 18% and Sonar has been told to accept it (quality profile exception CAB-2022-0917).
- **PLAT-1580** Idempotency store is Oracle, TTL enforced by a nightly job. Between expiry and the job running, a reused key returns the old result. 72h is really "72h to 96h".
- **PLAT-1499** Reversal of a posting that Bedrock accepted but we failed to record (adapter timeout after send) is a manual process. Runbook step 4.
- **PLAT-0722** Daily limit check reads the limit from a properties file per product type. Product team change it by raising a ticket to us.

## History

Built 2021 for the MOL transfer flow. The idempotency layer was added after INC0086001 where a
retrying mobile client posted the same transfer three times. Reversals came with Ledgerline in
2022. The service has had four owning teams; payments-platform inherited it in 2023 and the
Checkstyle ruleset has 900 open violations in it that nobody has time for (PLAT-1330).

See `docs/adr/` and `docs/runbooks/`.
