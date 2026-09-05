# Runbook: bff-business

Owner business-digital. Rota: business-digital, Chennai hours (10:00-21:00 IST) with payments-platform Jersey City picking up US afternoon. No overnight cover: business-web is not a 24x7 channel.

Port 4501. Health: `GET /health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=bff-business`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / bff-business`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/bff-business -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### approvals_queue_depth > 200

Somebody's approvers are on holiday. Not an incident; tell the business-digital product owner.

### entitlements-service unreachable

bff-business fails closed: every entitlement check returns 403. Expected. Fix entitlements-service.

## Restart

`oc rollout restart deploy/bff-business -n <ns>`. Safe at any time.

## Contacts

Slack `#plat-bff`. Escalation through PagerDuty, not DMs.
