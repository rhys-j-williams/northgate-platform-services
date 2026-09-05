# documents-service

Node 18.19.0, Express 4, TypeScript | port 4518 | version 1.14.3 | owner **retail-digital** (b.arceneaux, d.okafor)

Statements, tax documents and disclosure HTML for retail-web. Statement PDFs are rendered by
statements-api (Python, 4519) and streamed through here; the first request for a period tees
the stream into the object store and later requests are served from there
(`X-Meridian-Source: statements-api` vs `archive`). The object store is a local disk directory in
every environment below prod; prod is the enterprise object store behind the same interface.
Tax documents are 1099-INT placeholders generated on first request. Disclosures are static HTML
in `disclosures/`, served with a version header that retail-web caches on.

Express, not Nest, because it was written by a different team in 2021 and nobody has had a
reason to change it.

## On call

retail-digital, Plano hours (08:00-18:00 CT). Statement availability is a 24h SLA, not a paging SLA.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/documents/v1/statements` | Periods per account for the token's customer, with `archived` flag. |
| `GET` | `/documents/v1/statements/{accountId}/{period}.pdf` | Read-through. 404 if the account is not the customer's. |
| `GET` | `/documents/v1/tax` | 1099-INT list for interest bearing accounts. |
| `GET` | `/documents/v1/tax/{year}/{accountId}.pdf` |  |
| `GET` | `/documents/v1/disclosures` | Public. No token. |
| `GET` | `/documents/v1/disclosures/{key}` | Public. `X-Disclosure-Version` header. |
| `GET` | `/health` | Reports store root and statements-api reachability. |

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

Port **4518**. `MERIDIAN_AUTH_MODE=insecure-local` skips JWKS validation and trusts the token
payload; it refuses to start under `NODE_ENV=production`. With the Keystone mock on 4400 leave it
unset. `.npmrc` carries `legacy-peer-deps` for the reasons in TOOL-0977; do not pass it on the
command line.

Coverage target in `sonar-project.properties`: 25%. `COVERAGE.md` at the platform root has the current number.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/documents-service` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1877** Statements and tax routes have no Jest coverage; stubbing statements-api properly is the ticket. They are covered by `smoke.sh`.
- **MOL-2210** A statements-api failure mid-stream leaves a partial file in the store. `put()` writes to a temp name and renames, so a partial is not served, but it is not cleaned up either. Nightly job in prod, nothing locally.
- **MOL-1755** Tax placeholders are not real 1099s. The real ones come from the tax vendor feed in January and are dropped into the store by a batch job that lives in a different repository.
- **PLAT-1330** Disclosure HTML is served with `bypassSecurityTrust` on the front end side (cn-disclosure in canopy-ui). Not our code, but every security review ends up here asking about it.

## History

Written 2021 by the Plano team for the paperless statements programme. The object store stub was
meant to be replaced by the enterprise store SDK in 2022; the interface was designed for it and
the SDK never got an internal Artifactory mirror (TOOL-0915), so the local disk implementation is
what runs in dev and uat to this day.

See `docs/adr/` and `docs/runbooks/`.
