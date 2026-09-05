# statements-api

Python 3.11, FastAPI | port 4519 | version 0.9.4 | owner **payments-platform** (a.balaraman, j.hollins)

Renders statement PDFs with ReportLab from Bedrock transaction data (via bedrock-adapter, with a
fixture fallback when the adapter is unreachable). Groups posted transactions by calendar month,
computes closing balances, lays out the bank header, summary table, transactions and the
disclosure footer. Masks account numbers. Uses Helvetica because the brand font licence does not
cover server rendering (BRAND-0112).

**There is no test framework, there are no tests and the Jenkinsfile has no test stage.** It
was written as a two-week spike in 2021 to unblock the paperless statements pilot and went to
prod as the spike. Every attempt to add pytest has been deprioritised against a "rewrite in
Java" that has not happened either. The quality gate exception is CAB-2021-1188 and it is
renewed every year with decreasing enthusiasm.

## On call

No rota. Raise in #plat-statements; a.balaraman or whoever is on PLAT-BEACON picks it up. This is on the list for a rota (PLAT-1650) because documents-service depends on it.

## Endpoints

| Method | Path | Notes |
|---|---|---|
| `GET` | `/statements/v1/accounts/{accountId}/periods` | Available months with transaction counts and closing balance. |
| `GET` | `/statements/v1/accounts/{accountId}/{period}.pdf` | `period` is `YYYY-MM`. 400 on a bad period, 404 on an unknown account. |
| `GET` | `/health` | Reports whether fixture fallback is on. |

Errors use the platform envelope: `{code, message, status, correlationId, timestamp, violations[]}`.
Send `X-Correlation-Id`; if you do not, one is minted and returned.

## Build and run locally

```
./run.sh           # creates ~/.venvs/statements-api, pip installs the pinned requirements, starts uvicorn on 4519
```

The venv is deliberately outside the repository: `scripts/check-forbidden-strings.sh` walks the
worktree and site-packages are full of things it does not like.

No coverage number. No tests. See Known Issues.

## Deploy

`Jenkinsfile` in this directory calls the shared library. Chart is `helm/` here for local values
and the deployable chart is `platform-tooling/helm/statements-api` (TOOL-1102 to consolidate, open since
2023). Images come from `platform-tooling/docker/`; the `Dockerfile` here is the thin per-service
layer the pipeline actually builds.

## Known issues

- **PLAT-1650** No tests, no rota, no metrics beyond the access log. Read the paragraph above.
- **PLAT-1312** Statement period is the calendar month. Bedrock accounts have a cycle day (`MTBACCT.CYCLE-DAY`) and the paper statements use it. Customers with a mid-month cycle see different totals on paper vs online. There is a TODO in `periods.py`.
- **PLAT-1499** Rendering is synchronous in the request thread. Uvicorn with 4 workers handles the load in uat; prod has 2 replicas and nobody has load tested it.
- **BRAND-0112** Helvetica. Marketing have noticed.

## History

Spike, Sept 2021, two weeks, one engineer who has since left. The Bedrock client and fixture
fallback were added by documents-service's team in 2022 so they could develop without MQ. The
`requirements.txt` was pinned in the 2023 bump train and has not moved since.

See `docs/adr/` and `docs/runbooks/`.
