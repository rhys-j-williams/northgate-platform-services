# Runbook: alerts-preferences-service

Owner payments-platform. Rota: PLAT-BEACON rota (shared with Beacon). Business hours only for non-regulatory issues.

Port 4511. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=alerts-preferences-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / alerts-preferences-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/alerts-preferences-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### alert_pref_outbox_depth > 1000

Kafka is down or slow. Preferences are still being saved. Check Redpanda/Kafka; the outbox drains itself.

### REGULATORY_LOCK 409 spike

Usually a front end release that lets customers toggle something they should not. Not our bug, but we are the ones who see it.

## Restart

`oc rollout restart deploy/alerts-preferences-service -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-alerts`. Escalation through PagerDuty, not DMs.
