# iris-orchestrator

Node 18.19.0, NestJS 9 | port 4517 | version 2.8.1 | owner **retail-digital** (m.calderon, b.arceneaux)

Scripted intent engine behind the Iris chat widget. Intents, keywords and responses live in
`src/intents/intents.yaml`, loaded once at startup (there was a hot reload; see INC0097712 and
never again). Balance and transaction intents call bff-retail with the customer's own token.
Dispute and human handoff intents push a ticket onto the Redis handoff list
(`iris:handoff:v1`), which the agent desktop polls. Three consecutive fallback misses force a
handoff. Sessions are in Redis with a 30 minute TTL; in-memory when Redis is unavailable.

## On call

retail-digital. Iris is best-effort; outage degrades to the "chat unavailable" tile in retail-web. No overnight paging.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `POST` | `/iris/v1/sessions` | Requires a Keystone token. Session is bound to the token's customer. |
| `POST` | `/iris/v1/sessions/{id}/messages` | Returns the matched intent, confidence, reply text and any `handoff`. |
| `GET` | `/iris/v1/intents` | The YAML summary. Used by the widget team to check what shipped. |
| `GET` | `/iris/v1/handoff/queue` | Depth and mode (redis/memory). Not auth'd locally, is auth'd behind the Route in uat/prod. |
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

Port **4517**. `MERIDIAN_AUTH_MODE=insecure-local` skips JWKS validation and trusts the token
payload; it refuses to start under `NODE_ENV=production`. With the Keystone mock on 4400 leave it
unset. `.npmrc` carries `legacy-peer-deps` for the reasons in TOOL-0977; do not pass it on the
command line.

Coverage target in `sonar-project.properties`: 40%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/iris-orchestrator` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **IRIS-0412** Intent matching is keyword scoring with weights. It is not NLP and the product team keep asking why it does not understand "wot is my balence". There is a spelling normaliser TODO in `intent-matcher.ts`.
- **IRIS-0530** Reg E dispute response text is in the YAML, not in documents-service disclosures. Compliance want a single source. IRIS-0530 has been "in refinement" since 2023.
- **IRIS-0388** In-memory handoff mode loses tickets on restart. `IRIS_HANDOFF_STRICT=true` makes it fail instead, which is what uat/prod set.

## History

Iris launched in 2022 as a scripted FAQ bot to take pressure off the contact centre. The YAML
schema is on version 7. INC0097712 (Feb 2023): a hot reload of a malformed YAML took every intent
offline for 40 minutes on a Saturday; reload was removed and a startup validator added the
following week.

See `docs/adr/` and `docs/runbooks/`.
