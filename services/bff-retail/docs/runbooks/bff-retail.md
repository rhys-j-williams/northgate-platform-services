# Runbook: bff-retail

Owner retail-digital. Rota: retail-digital follow-the-sun: Charlotte 08:00-19:00 ET, Chennai (payments-platform covering) overnight. Escalation via the MOL on-call rota in PagerDuty.

Port 4500. Health: `GET /health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=bff-retail`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / bff-retail`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/bff-retail -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### bff_retail_upstream_error_rate{upstream="bedrock-adapter"} > 2%

Bedrock is the only upstream that fails the accounts call outright. Check bedrock-adapter health first, then MQ.

### Redis unavailable

Service logs `cache.mode=memory` once and keeps going. Restore Redis; no restart needed, the client reconnects.

### 401 spike after Keystone deployment

They rotated the signing key. Restart the pods (PLAT-1233). Yes, really.

## Restart

`oc rollout restart deploy/bff-retail -n <ns>`. Safe at any time.

## Contacts

Slack `#plat-bff`. Escalation through PagerDuty, not DMs.
