# Runbook: beacon-notifications

Owner payments-platform. Rota: PagerDuty PLAT-BEACON. Regulatory alert delivery has a 4h SLA with Compliance, so this rota is real and it does page at night.

Port 4510. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=beacon-notifications`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / beacon-notifications`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/beacon-notifications -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### beacon_sequence_parked > 50

Events waiting on a gap. Usually one producer instance has stalled. Check `BEACON_SEQUENCE` for customers with `LAST_SEQ` far behind `MAX_SEEN`.

### Regulatory alert dispatch failure

Any `REG_*` alert code failing dispatch pages Compliance on-call as well as us. Retry from `BEACON_DISPATCH` with status `FAILED` using the admin endpoint. Do not re-send manually by email.

### LDAP health DOWN

Locally: the in-memory server did not start, check the LDIF. In uat/prod: the corporate LDAP is unreachable, admin endpoints 503, dispatch continues.

## Restart

`oc rollout restart deploy/beacon-notifications -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-beacon`. Escalation through PagerDuty, not DMs.
