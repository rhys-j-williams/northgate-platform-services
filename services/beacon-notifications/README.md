# beacon-notifications

Java 11, Spring Boot 2.7.18 | port 4510 | version 5.2.0 | owner **payments-platform** (p.venkatesan, j.hollins)

**Compliance critical.** SOX/PCI in scope. Changes need a CAB reference and a GIS AppSec reviewer (CODEOWNERS enforces the second).

Beacon is the real time alerting engine. It consumes account events off `ACCT.EVENTS` (IBM MQ;
Artemis under `local-artemis`), evaluates the customer's preferences (from
alerts-preferences-service, with a fixture fallback), renders the template, including the
regulatory disclosure footer where the alert code requires one, and dispatches through channel
adapters (email, sms, push, in-app, console). Delivery attempts land in Oracle (`BEACON_DISPATCH`).

The thing to understand before touching it: **per-customer ordering**. Events carry a
`sequenceNumber` per customer and Beacon will not dispatch sequence N+1 before N has been
dispatched or expired. This is what stopped "your balance is $500" arriving after "your balance
is $20" (INC0084410, 2021, a Compliance finding). The gate lives in `SequenceCoordinator` and it has
no unit tests. It has an integration test that was disabled in 2023 because it was flaky on the
shared Jenkins agents (PLAT-1288). Nobody has re-enabled it.

## On call

PagerDuty PLAT-BEACON. Regulatory alert delivery has a 4h SLA with Compliance, so this rota is real and it does page at night.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/beacon/v1/events` | Direct injection for local testing and the smoke test. Same payload as the MQ message. |
| `GET` | `/beacon/v1/customers/{customerId}/dispatches` | What went out, in order, with channel and status. |
| `GET` | `/beacon/v1/admin/templates` | LDAP admin auth (in-memory UnboundID locally, `beacon-ldap.ldif`). Group `beacon-admins`. |
| `GET` | `/actuator/health` | Includes MQ, Oracle/H2 and LDAP components. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4510**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 25%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/beacon-notifications` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1288** Ordering integration test disabled. See above. The sequence gate is exercised only by the estate smoke test.
- **PLAT-1544** Out-of-order events with a gap (N then N+2) wait for `beacon.sequence.gap-timeout` (default 30s) and then dispatch anyway. Compliance signed this off in CAB-2023-0412. It still makes people nervous.
- **PLAT-0994** Templates are Thymeleaf text templates loaded at startup. Changing a regulatory footer is a deployment. There is a design for a template store; there is no ticket for building it.
- **PLAT-1602** The console channel adapter is what the smoke test observes. It is also what a misconfigured prod pod would fall back to. There is a startup check that refuses `console` in the `prod` profile.

## History

Beacon replaced a vendor alerting product in 2021 (the vendor contract lapse is why it was built
in eight weeks and why the first version had no ordering). Ordering was added in Q4 2021 after
INC0084410. The LDAP admin surface was added for the operations team in 2022 so they could see
templates without a database login. Oracle has been the store from day one because the alerting
data classification put it in the same zone as the customer master.

There is a Lambda landing zone for Beacon in `platform-tooling/terraform/` with no code in it. The
bank has been going to move Beacon to AWS since 2023.

See `docs/adr/` and `docs/runbooks/`.
