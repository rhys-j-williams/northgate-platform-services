#!/usr/bin/env bash
# Start / stop the thirteen platform services as plain local processes, no Docker.
#
#   scripts/run-local.sh start [name ...]     everything, or just the named services
#   scripts/run-local.sh stop  [name ...]
#   scripts/run-local.sh status
#
# Order matters a little: bedrock-adapter before anything that reads balances, statements-api before
# documents-service, entitlements before bff-business. We start in that order and do not wait
# between services; every caller has a fixture fallback so a slow neighbour just means a few
# seconds of synthetic numbers (MERIDIAN_FIXTURE_FALLBACK=true is the local default).
#
# mock-external must already be up (../mock-external/estate-up.sh or `make up` there). Without it
# the services still start; the BFFs will log JWKS fetch failures until Keystone appears.
#
# Jars are expected in target/ (make build). Node services run dist/ (npm run build). Python venvs
# live under ~/.venvs (PLAT-1933, see Makefile). PIDs and logs go to var/.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VAR="$ROOT/var"
mkdir -p "$VAR/log"

JAVA11="${JAVA11:-/usr/lib/jvm/java-11-openjdk-amd64}"
JAVA17="${JAVA17:-/usr/lib/jvm/java-17-openjdk-amd64}"
VENVS="${VENVS:-$HOME/.venvs}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local,local-artemis}"
export MERIDIAN_FIXTURES="${MERIDIAN_FIXTURES:-$ROOT/fixtures/meridian-fixtures.json}"
export MERIDIAN_FIXTURE_FALLBACK="${MERIDIAN_FIXTURE_FALLBACK:-true}"
# estate-up.sh exports KEYSTONE_JWKS_URI; the Node services read KEYSTONE_JWKS_URL. Both spellings
# have shipped and nobody wants to be the one to break the other side (PLAT-2604).
export KEYSTONE_JWKS_URL="${KEYSTONE_JWKS_URL:-${KEYSTONE_JWKS_URI:-http://localhost:4400/.well-known/jwks.json}}"
export MERIDIAN_SECURITY_JWKS_URI="${MERIDIAN_SECURITY_JWKS_URI:-$KEYSTONE_JWKS_URL}"
export MERIDIAN_SECURITY_ISSUER="${MERIDIAN_SECURITY_ISSUER:-${KEYSTONE_ISSUER:-http://localhost:4400}}"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="${SPRING_KAFKA_BOOTSTRAP_SERVERS:-${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}}"

# name:port:kind  (kind = java11 | java17 | node | python)
ORDER=(
  bedrock-adapter:4516:java11
  beacon-notifications:4510:java11
  alerts-preferences-service:4511:java11
  txn-posting-service:4512:java11
  pii-vault-service:4513:java11
  audit-trail-service:4514:java11
  entitlements-service:4515:java17
  statements-api:4519:python
  exposure-calc:4520:python
  bff-retail:4500:node
  bff-business:4501:node
  iris-orchestrator:4517:node
  documents-service:4518:node
)

use_node() {
  # shellcheck disable=SC1091
  [ -s "$HOME/.nvm/nvm.sh" ] && . "$HOME/.nvm/nvm.sh" && nvm use 18.19.0 >/dev/null 2>&1 || true
}

command_for() { # name port kind
  local name="$1" port="$2" kind="$3" dir="$ROOT/services/$1" jar
  case "$kind" in
    java11|java17)
      local jh="$JAVA11"; [ "$kind" = java17 ] && jh="$JAVA17"
      jar=$(ls "$dir"/target/*.jar 2>/dev/null | grep -v -E 'sources|javadoc|original' | head -1 || true)
      if [ -z "$jar" ]; then echo "$name: no jar in target/, run make build" >&2; return 1; fi
      echo "cd '$dir' && exec '$jh/bin/java' \${JAVA_OPTS:-} -jar '$jar' --server.port=$port" ;;
    node)
      # Nest services emit dist/main.js; documents-service is plain Express and emits dist/server.js
      local entry="dist/main.js"; [ -f "$dir/dist/server.js" ] && entry="dist/server.js"
      if [ ! -f "$dir/$entry" ]; then echo "$name: no $entry, run make build" >&2; return 1; fi
      echo "cd '$dir' && PORT=$port exec node $entry" ;;
    python)
      local py="$VENVS/$name/bin/python"
      if [ ! -x "$py" ]; then echo "$name: no venv at $VENVS/$name, run make install" >&2; return 1; fi
      echo "cd '$dir' && PORT=$port exec '$py' -m uvicorn app.main:app --host 0.0.0.0 --port $port" ;;
  esac
}

selected() { # filters ORDER by the names given on the command line, or all
  local want=("$@")
  for entry in "${ORDER[@]}"; do
    IFS=: read -r name port kind <<<"$entry"
    if [ ${#want[@]} -eq 0 ]; then echo "$entry"; continue; fi
    for w in "${want[@]}"; do [ "$w" = "$name" ] && echo "$entry"; done
  done
}

is_running() { local pf="$VAR/$1.pid"; [ -f "$pf" ] && kill -0 "$(cat "$pf")" 2>/dev/null; }

start() {
  use_node
  while IFS=: read -r name port kind; do
    [ -n "$name" ] || continue
    if is_running "$name"; then echo "$name already running (pid $(cat "$VAR/$name.pid"))"; continue; fi
    cmd=$(command_for "$name" "$port" "$kind") || continue
    echo "starting $name on :$port"
    nohup bash -c "$cmd" >>"$VAR/log/$name.log" 2>&1 &
    echo $! > "$VAR/$name.pid"
  done < <(selected "$@")
}

stop() {
  while IFS=: read -r name port kind; do
    [ -n "$name" ] || continue
    if is_running "$name"; then
      pid=$(cat "$VAR/$name.pid")
      echo "stopping $name (pid $pid)"
      kill "$pid" 2>/dev/null || true
      for _ in 1 2 3 4 5 6 7 8 9 10; do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$VAR/$name.pid"
  done < <(selected "$@")
}

status() {
  printf '%-28s %-6s %-8s %s\n' service port pid health
  while IFS=: read -r name port kind; do
    [ -n "$name" ] || continue
    pid="-"; is_running "$name" && pid=$(cat "$VAR/$name.pid")
    health="down"
    for path in /actuator/health /health /healthz; do
      code=$(curl -s -o /dev/null --max-time 2 -w '%{http_code}' "http://localhost:$port$path" || true)
      case "$code" in 200|503) health="$code $path"; break;; esac
    done
    printf '%-28s %-6s %-8s %s\n' "$name" "$port" "$pid" "$health"
  done < <(selected)
}

case "${1:-}" in
  start) shift; start "$@" ;;
  stop) shift; stop "$@" ;;
  status) status ;;
  *) echo "usage: $0 start|stop|status [service ...]" >&2; exit 2 ;;
esac
