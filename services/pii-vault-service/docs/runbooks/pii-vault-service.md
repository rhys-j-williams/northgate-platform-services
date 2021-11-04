# Runbook: pii-vault-service

Owner payments-platform. Rota: PLAT-PII. Business hours, payments-platform. GIS AppSec (c.mbeki, v.orlova) are mandatory reviewers on every change and are on the escalation path.

Port 4513. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=pii-vault-service`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / pii-vault-service`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/pii-vault-service -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### Vault unreachable

Tokenise/detokenise return 503. No fallback in uat/prod, by design. Check the Vault agent sidecar then the Vault cluster status page.

### detokenise call volume > 10/min

Something is bulk detokenising. That is not a normal pattern. Identify the caller from the access log and involve GIS before doing anything else.

## Restart

`oc rollout restart deploy/pii-vault-service -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-pii`. Escalation through PagerDuty, not DMs.
