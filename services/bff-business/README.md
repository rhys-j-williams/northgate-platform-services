# bff-business

Node 18.19.0, NestJS 9 | port 4501 | version 4.12.0 | owner **business-digital** (k.subramani, n.rajaram)

BFF for business-web and ledgerline-web. Same bones as bff-retail (and yes, the same copied
`auth/` and `common/` folders, see ADR 0003 over there) but the interesting part is entitlements:
every call checks the operator's role against entitlements-service, and money-moving actions go
into an approvals queue that a second operator has to release. The approvals queue is the thing
that gets us paged.

## On call

business-digital, Chennai hours (10:00-21:00 IST) with payments-platform Jersey City picking up US afternoon. No overnight cover: business-web is not a 24x7 channel.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/v1/organisations/{orgId}/accounts` | Requires `business:accounts:read` on the org. Entitlement decision cached 60s. |
| `POST` | `/api/v1/payments` | Creates a payment pending approval. Never posts directly. |
| `GET` | `/api/v1/approvals` | The queue, for operators with `business:approvals:review`. |
| `POST` | `/api/v1/approvals/{id}/decision` | approve/reject. Maker-checker enforced: the approver cannot be the maker. |
| `GET` | `/health` |  |

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

Port **4501**. `MERIDIAN_AUTH_MODE=insecure-local` skips JWKS validation and trusts the token
payload; it refuses to start under `NODE_ENV=production`. With the Keystone mock on 4400 leave it
unset. `.npmrc` carries `legacy-peer-deps` for the reasons in TOOL-0977; do not pass it on the
command line.

Coverage target in `sonar-project.properties`: 30%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/bff-business` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **MBZ-1304** Approvals queue is in memory when Redis is unavailable. In-flight approvals are lost on restart in that mode. There is a warning log; there is not a metric.
- **MBZ-1411** Entitlement cache is keyed by operator only, not operator+org. An operator with two orgs can see a stale decision for 60s after a role change.
- **LDG-0877** ledgerline-web sends `X-Requested-With` on preflight and our CORS config only whitelists it in dev. Prod goes through the WAF which strips it, so it works by accident.

## History

Forked from bff-retail in March 2022 (MBZ-0801) by copying the directory, which the git history
still shows. Approvals were bolted on for the Ledgerline launch in Q4 2022. The dual approval rules
moved to entitlements-service in 2024 (PLAT-1352); this service still carries the old
`ApprovalPolicy` class, marked deprecated, because two ledgerline screens call the old shape.

See `docs/adr/` and `docs/runbooks/`.
