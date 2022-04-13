# Runbook: documents-service

Owner retail-digital. Rota: retail-digital, Plano hours (08:00-18:00 CT). Statement availability is a 24h SLA, not a paging SLA.

Port 4518. Health: `GET /health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=documents-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / documents-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/documents-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### STATEMENTS_API_UNAVAILABLE 503 rate

statements-api is down. Archived statements still serve. Check 4519 health, then the Python pod logs; ReportLab font errors are the usual.

### Object store disk > 85%

dev/uat only. Delete anything older than 90 days from the store root; it will re-render.

## Restart

`oc rollout restart deploy/documents-service -n <ns>`. Safe at any time.

## Contacts

Slack `#plat-documents`. Escalation through PagerDuty, not DMs.
