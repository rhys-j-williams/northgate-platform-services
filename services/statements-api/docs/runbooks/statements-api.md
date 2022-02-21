# Runbook: statements-api

Owner payments-platform. Rota: No rota. Raise in #plat-statements; a.balaraman or whoever is on PLAT-BEACON picks it up. This is on the list for a rota (PLAT-1650) because documents-service depends on it.

Port 4519. Health: `GET /health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=statements-api`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / statements-api`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/statements-api -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### 5xx on .pdf

Look at the log. If it is a ReportLab `LayoutError` a transaction description is too long for the table cell; that is PLAT-1704 and the fix is to truncate in `pdf.py`. If it is a connection error, bedrock-adapter is down and fixture fallback is off.

### Nothing else

There are no other alerts because there are no metrics. See PLAT-1650.

## Restart

`oc rollout restart deploy/statements-api -n <ns>`. Safe at any time.

## Contacts

Slack `#plat-statements`. Escalation through PagerDuty, not DMs.
