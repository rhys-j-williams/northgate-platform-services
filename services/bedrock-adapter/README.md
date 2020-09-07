# bedrock-adapter

Java 11, Spring Boot 2.7.18 | port 4516 | version 3.14.2 | owner **payments-platform** (j.hollins, p.venkatesan)

**Compliance critical.** SOX/PCI in scope. Changes need a CAB reference and a GIS AppSec reviewer (CODEOWNERS enforces the second).

Translates REST calls from the BFFs and the posting service into Bedrock CICS-style fixed width
messages, puts them on `BEDROCK.REQ.ACCTINQ` / `BEDROCK.REQ.TXNPOST` / `BEDROCK.REQ.CUSTPROF`,
correlates the reply off `BEDROCK.RESP.*` and turns the copybook record back into JSON. Nothing in
the estate reads Bedrock without going through here, which is both the point and the problem.
The record layouts are the copybooks in `platform-services/copybooks/`; the signed zoned decimal
overpunch is implemented once in `@meridian/domain-fixtures` (`bedrock.ts`) and the Java side has
to agree with it byte for byte. `FixedWidthCodecTest` pins that.

## On call

PagerDuty schedule PLAT-BEDROCK (payments-platform primary, mainframe integration team secondary 22:00-06:00 ET)

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/bedrock/v1/accounts/{accountNumber}` | ACCT-INQ round trip. Fixture fallback when MQ is down and `bedrock.fixture-fallback=true`. |
| `GET` | `/bedrock/v1/accounts/{accountNumber}/transactions` | Paged off the fixture transaction set or the TRAN reply block. `from`/`to` are inclusive posting dates. |
| `GET` | `/bedrock/v1/customers/{customerId}` | CUST-PROF. Masks SSN unless the caller has `bedrock:pii` scope. |
| `POST` | `/bedrock/v1/postings` | TXN-POST. Idempotency is the caller's job (txn-posting-service), we only forward the key in the header block. |
| `GET` | `/actuator/health` | MQ connection state is under `components.jms`. Amber when on Artemis. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
# common-starter must be in ~/.m2 first (make install at platform-services/ does this)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -q verify                       # unit tests + JaCoCo report in target/site/jacoco
SPRING_PROFILES_ACTIVE=local,local-artemis mvn spring-boot:run
```

Runs on **4516**. `local` gives you H2, `local-artemis` swaps IBM MQ for embedded Artemis on
61616. Against the real MQ mock on 1414 drop the second profile. Nothing in the code checks which
broker it is on and it must stay that way (see the platform README).

Coverage target in `sonar-project.properties`: 20%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/bedrock-adapter` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1418** Reply correlation uses JMSCorrelationID only. Bedrock occasionally echoes an empty one on ACCT-INQ timeouts and the reply sits on the queue until the reaper clears it (`bedrock.reply-reaper.interval`).
- **PLAT-1522** Negative balances on `MTBACCT` come back overpunched in the last digit. Everyone knows. Do not "fix" the parser, the fixtures are right and the copybook is right, it is the mainframe test region that has the bad data.
- **PLAT-0891** Artemis fallback does not honour MQ message expiry, so soak tests on the `local-artemis` profile leak messages. Restart the container.
- **PLAT-1607** The EBCDIC/ASCII code page assumption (CP037 in, ISO-8859-1 out) is hard coded in `FixedWidthCodec`. Bedrock UAT is on CP1140 since the 2023 upgrade. Nobody has hit it because we do not send currency symbols.

## History

The adapter was the first Java service in platform-services (scaffolded Nov 2020 as a Spring Boot
2.3 app, upgraded to 2.7 in the 2023 bump train). It replaced a Tuxedo bridge. The copybooks were
extracted from the Bedrock team's PDS by hand in 2020 and we have never been given write access
since, so if a layout changes we find out when parsing breaks (INC0091132, Aug 2022, `MTBTRAN`
grew a 2 byte channel code and every transaction description was shifted).

See `docs/adr/` and `docs/runbooks/`.
