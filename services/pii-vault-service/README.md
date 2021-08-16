# pii-vault-service

Java 11, Spring Boot 2.7.18 | port 4513 | version 1.12.0 | owner **payments-platform** (p.venkatesan, c.mbeki)

**Compliance critical.** SOX/PCI in scope. Changes need a CAB reference and a GIS AppSec reviewer (CODEOWNERS enforces the second).

Compliance critical. Format-preserving tokenisation of PII (SSN, card PAN, account numbers)
so that downstream systems can carry a token that looks like the original. Keys come from Vault
(the mock on 4605 locally, transit engine path `transit/keys/pii-fpe-v2`). Every tokenise and
detokenise call is written to `PII_ACCESS_LOG` with the caller identity from the JWT, and that
log is what the quarterly access review reads.

Coverage is 8%. The FPE algorithm has a known-answer test and that is essentially it. GIS
accepted this in 2022 on the basis that the algorithm is a vetted implementation and the rest is
plumbing; the risk acceptance (RA-2022-0341) expires in 2026 and nobody has started on renewing it.

## On call

PLAT-PII. Business hours, payments-platform. GIS AppSec (c.mbeki, v.orlova) are mandatory reviewers on every change and are on the escalation path.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/pii/v1/tokenise` | `{"type":"ssn|pan|account","value":"..."}`. Requires `pii:tokenise`. |
| `POST` | `/pii/v1/detokenise` | Requires `pii:detokenise`, which almost nothing has. Logged with `purpose` from the request body, mandatory. |
| `GET` | `/pii/v1/access-log` | `pii:audit` scope. Paged. |
| `GET` | `/actuator/health` | Vault connectivity under `components.vault`. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4513**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 8%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/pii-vault-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1155** Key rotation is manual. `pii-fpe-v2` has been current since 2023. Tokens carry the key version so rotation is possible; nobody has done one.
- **GIS-2044** Access log writes are asynchronous. A crash between tokenise and log write loses the log entry. GIS know. Fix is the outbox pattern from alerts-preferences, PLAT-1611, not started.
- **PLAT-1388** Luhn-preserving PAN tokens: the fixtures all fail Luhn on purpose, so the "preserve Luhn validity" property is only tested for the invalid case. Real PANs are valid. Think about that before relying on the test.
- **PLAT-0899** Vault mock fallback: with `pii.vault.fallback-key` set the service uses a static key. Refuses to start with it in `prod`. Refused to start in uat once too (INC0099120) because someone copied the dev values file.

## History

Written 2022 for the PCI scope reduction programme. It has had one security fix from GIS
(GIS-1877, timing-safe comparison on the token lookup) and otherwise very few changes, which is
either a sign of stability or of nobody wanting to touch it.

See `docs/adr/` and `docs/runbooks/`.
