# Runbook: txn-posting-service

Owner payments-platform. Rota: PLAT-POSTING. 24x7, primary in Jersey City, secondary Chennai. This is a SOX in-scope service; changes need a CAB reference.

Port 4512. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=txn-posting-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / txn-posting-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/txn-posting-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### posting_bedrock_timeout

Adapter did not reply. Posting is in state `UNKNOWN`. Reconcile against Bedrock the next morning using the ACCT-INQ transaction list; do NOT retry blindly, the key may have been consumed on the mainframe side.

### IDEMPOTENCY_CONFLICT rate > 0.1%

A client is reusing keys with different bodies. Find the client from the correlation id; it is nearly always a front end bug.

### Reversal failed, posting recorded

Step 4: raise a Bedrock manual adjustment (MAINT ticket) and set the posting to `REVERSAL_MANUAL` via SQL. Two-person rule applies, get someone on the call.

## Restart

`oc rollout restart deploy/txn-posting-service -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-txn`. Escalation through PagerDuty, not DMs.
