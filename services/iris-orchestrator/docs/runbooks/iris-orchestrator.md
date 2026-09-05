# Runbook: iris-orchestrator

Owner retail-digital. Rota: retail-digital. Iris is best-effort; outage degrades to the "chat unavailable" tile in retail-web. No overnight paging.

Port 4517. Health: `GET /health`. Logs are JSON to stdout, indexed in Splunk as
`index=cswt sourcetype=platform-services service=iris-orchestrator`; search by `correlationId` first,
always. Dashboards: Grafana folder `CSWT / platform-services / iris-orchestrator`.

## First five minutes

1. Is it a deployment? `oc rollout history deploy/iris-orchestrator -n cswt-prod`. If the last rollout is
   inside the incident window, roll back and think later.
2. Is it a dependency? Health endpoint lists components. Down component, go to that runbook.
3. Is it everyone? Check bff-retail / bff-business health; if they are red too, it is probably
   Bedrock or Keystone, join the bridge.

## Alerts

### iris_handoff_queue_depth > 100

Agent desktop is not polling or all agents are busy. Not an Iris fault; tell the contact centre duty manager.

### Intent load failed at startup

Somebody merged bad YAML. Pod will not start; previous ReplicaSet keeps serving. Revert the YAML change.

## Restart

`oc rollout restart deploy/iris-orchestrator -n <ns>`. Safe at any time.

## Contacts

Slack `#plat-iris`. Escalation through PagerDuty, not DMs.
