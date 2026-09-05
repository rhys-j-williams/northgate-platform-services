# Runbook: audit-trail-service

Owner platform-engineering. Rota: PLAT-AUDIT, business hours. Audit is not customer facing; a backlog on the Kafka topic is tolerable for hours, a hash chain break is not.

Port 4514. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=audit-trail-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / audit-trail-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/audit-trail-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### audit_hash_chain_broken

P1. Do not restart anything. Capture the `verify` output, page platform-engineering lead and the SOX control owner. It has never fired for real; both times were PLAT-1466.

### Kafka consumer lag > 100000

Audit is behind. Check consumer is connected (`components.kafka`). If connected and lagging, scale to 3 pods; the topic has 6 partitions.

## Restart

`oc rollout restart deploy/audit-trail-service -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-audit`. Escalation through PagerDuty, not DMs.
