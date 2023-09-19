# Runbook: entitlements-service

Owner identity-platform. Rota: identity-platform, Chester (08:00-18:00 UK). US afternoon covered by payments-platform Jersey City under the shared services agreement. This is the only Boot 3 service and the on-call notes assume you know that.

Port 4515. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=entitlements-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / entitlements-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/entitlements-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### entitlement_check_latency_p99 > 200ms

Cache is off or evicting. `entitlements.cache.ttl` should be 60s. Check pod memory; Caffeine under memory pressure evicts hard.

### SAME_ACTOR 409 rate

A UI is letting makers approve their own items. It is a UI bug, not ours, but Compliance will ask us first.

## Restart

`oc rollout restart deploy/entitlements-service -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-entitlements`. Escalation through PagerDuty, not DMs.
