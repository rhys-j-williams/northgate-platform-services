# bff-retail

Node 18.19.0, NestJS 9 | port 4500 | version 3.9.1 | owner **retail-digital** (d.okafor, m.calderon)

Backend-for-frontend for retail-web (Meridian Online). Validates Keystone JWTs against the mock
JWKS on 4400, fans out to bedrock-adapter (balances, transactions), Aggregio (external accounts),
TickerHaus (market data on the dashboard), TriScore (credit score widget) and PayLink (P2P), and
hands the front end one shape per screen under `/api/v1`. Redis on 6379 caches the aggregation
per customer for 30s; when Redis is down we fall back to an in-process LRU and carry on, which
has saved us at least twice (INC0101877).

## On call

retail-digital follow-the-sun: Charlotte 08:00-19:00 ET, Chennai (payments-platform covering) overnight. Escalation via the MOL on-call rota in PagerDuty.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/v1/me` | Customer profile from the token claims plus the fixture customer record. |
| `GET` | `/api/v1/accounts` | Bedrock-backed balances for every account on the customer. Cached. |
| `GET` | `/api/v1/accounts/{id}/transactions` | Paged. Filter chips on the front end map to `type`, `from`, `to`, `minAmount`. |
| `GET` | `/api/v1/dashboard` | The fan-out. Any single upstream failing degrades its tile to `unavailable: true` rather than failing the call. |
| `POST` | `/api/v1/transfers` | Delegates to txn-posting-service. Requires `mol:transfer` scope. |
| `GET` | `/health` | Redis and each upstream, individually. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
nvm use            # .nvmrc says 18.19.0
npm ci
npm run lint
npm test           # jest, coverage in coverage/
npm run build
MERIDIAN_AUTH_MODE=insecure-local npm start
```

Port **4500**. `MERIDIAN_AUTH_MODE=insecure-local` skips JWKS validation and trusts the token
payload; it refuses to start under `NODE_ENV=production`. With the Keystone mock on 4400 leave it
unset. `.npmrc` carries `legacy-peer-deps` for the reasons in TOOL-0977; do not pass it on the
command line.

Coverage target in `sonar-project.properties`: 35%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/bff-retail` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **MOL-2231** Dashboard fan-out has no per-upstream timeout budget; a slow TickerHaus makes the whole dashboard wait for the global 8s. Circuit breaker was written, never merged (PR 412, conflicts).
- **PLAT-1233** The Keystone JWKS is fetched at startup and cached forever. Key rotation requires a restart. Keystone rotate quarterly and forget to tell us.
- **MOL-1980** Transaction filter `type=fee` also matches `fee-reversal` because the match is a prefix. Front end works around it.
- **PLAT-1490** In-process LRU fallback is per pod, so cache coherence across replicas is gone while Redis is down. Acceptable, documented, nobody likes it.

## History

Started life in 2021 as a NestJS 7 app when the MOL rewrite kicked off; moved to NestJS 9 in the
2023 bump train (MOL-1877) which was mostly painless apart from the `@nestjs/axios` extraction.
Redis was added in mid 2022 after the dashboard took Bedrock down on a Monday morning
(INC0088340). The `insecure-local` auth mode exists so that the front end teams can run without
the Keystone mock; it refuses to start with that mode unless `NODE_ENV` is unset or `development`.

See `docs/adr/` and `docs/runbooks/`.
