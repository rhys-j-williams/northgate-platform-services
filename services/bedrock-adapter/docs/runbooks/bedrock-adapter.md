# Runbook: bedrock-adapter

Owner payments-platform. Rota: PagerDuty schedule PLAT-BEDROCK (payments-platform primary, mainframe integration team secondary 22:00-06:00 ET)

Port 4516. Health: `GET /actuator/health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=bedrock-adapter`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / bedrock-adapter`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/bedrock-adapter -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### BedrockReplyTimeoutRate > 5% for 5m

Check MQ channel status in the MQ console (9443 locally). If the channel is up, Bedrock is slow, not us; page the mainframe on-call. If `local-artemis` is active in a non-local environment, someone deployed with the wrong profile: roll back.

### BedrockCodecError

A reply did not parse. Pull the raw message from `BEDROCK.RESP.*.DLQ`, compare against the copybook. Nine times out of ten Bedrock changed a layout (see INC0091132).

### Fixture fallback active in uat/prod

This should be impossible; `bedrock.fixture-fallback` is false in the prod values. If you see the `X-Meridian-Source: fixture` header from a non-local environment, stop the pods and raise a P2.

## Restart

`oc rollout restart deploy/bedrock-adapter -n <ns>`. Safe at any time for the REST path; in-flight MQ messages are redelivered.

## Contacts

Slack `#plat-bedrock`. Escalation through PagerDuty, not DMs.
