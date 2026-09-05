#!/usr/bin/env bash
# platform-services smoke. Health on every port, then the paths the demo actually walks:
#
#   1  keystone        authorization code + PKCE as the fixture customer, MFA 123456
#   2  bedrock-adapter account inquiry comes back with the fixture routing number
#   3  bff-retail      GET /api/v1/accounts with that token, at least one account
#   4  beacon          three out of order events for one customer, dispatches come out in sequence
#   5  audit-trail     append, read back by subject, hash chain verifies
#   6  entitlements    anonymous 401, roles with a token
#   7  documents       latest statement PDF twice: first from statements-api, second from archive
#   8  statements-api  PDF is non-empty and starts with %PDF
#
# Exit code is the number of failures. Nothing here writes anything a rerun would trip over except
# the beacon debug ring, which we clear first. Set SMOKE_SKIP_MESSAGING=1 on a box where the
# mocks are not up: steps 1, 3, 6, 7 are skipped instead of failing.
#
# This is deliberately curl and python3 only. mock-external/smoke.sh has the estate wide version.
set -uo pipefail

KEYSTONE_URL="${KEYSTONE_URL:-http://localhost:4400}"
CLIENT_ID="${OIDC_CLIENT_ID:-meridian-online-web}"
REDIRECT_URI="${OIDC_REDIRECT_URI:-http://localhost:4200/index.html}"
FIXTURES="${MERIDIAN_FIXTURES:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/fixtures/meridian-fixtures.json}"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/ps-smoke.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

PASS=0; FAIL=0
ok()   { PASS=$((PASS + 1)); printf '  ok    %-16s %s\n' "$1" "$2"; }
fail() { FAIL=$((FAIL + 1)); printf '  FAIL  %-16s %s\n' "$1" "$2"; }
skip() { printf '  skip  %-16s %s\n' "$1" "$2"; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)" 2>/dev/null; }

echo "== health"
for entry in bff-retail:4500 bff-business:4501 beacon-notifications:4510 alerts-preferences:4511 txn-posting:4512 \
             pii-vault:4513 audit-trail:4514 entitlements:4515 bedrock-adapter:4516 iris-orchestrator:4517 \
             documents-service:4518 statements-api:4519 exposure-calc:4520; do
  IFS=: read -r name port <<<"$entry"
  code=""
  for path in /actuator/health /health /healthz; do
    c=$(curl -s -o "$TMP/h" --max-time 3 -w '%{http_code}' "http://localhost:$port$path" || true)
    if [ "$c" = "200" ]; then code="$c $path"; break; fi
    # Boot readiness groups say 503 while a broker health indicator is red; the pod is still serving
    if [ "$c" = "503" ] && grep -q '"status":"DOWN"' "$TMP/h"; then code="503 $path (degraded)"; fi
  done
  if [ -n "$code" ]; then ok "$name" "$code"; else fail "$name" "no health endpoint answered on :$port"; fi
done

# Pick a fixture customer that has an open checking account. Same one every run: the first.
read -r CUSTOMER ACCOUNT <<<"$(python3 - "$FIXTURES" <<'EOF'
import json, sys
d = json.load(open(sys.argv[1]))
accts = d.get("accounts", [])
for a in accts:
    if a.get("status") == "open" and "checking" in a.get("type", ""):
        print(a["customerId"], a["accountId"]); break
else:
    print(accts[0]["customerId"], accts[0]["accountId"])
EOF
)"
echo "== fixture subject: $CUSTOMER / $ACCOUNT"

echo "== 1 keystone"
# Authorization code + PKCE as the fixture customer, MFA 123456. A client_credentials token would
# be simpler but its sub is the client, and bff-retail keys everything off sub (PLAT-2604).
TOKEN=""
if [ "${SMOKE_SKIP_MESSAGING:-0}" = "1" ]; then
  skip keystone "SMOKE_SKIP_MESSAGING=1"
else
  USERNAME=$(curl -s --max-time 5 "$KEYSTONE_URL/debug/users" | json '[u["username"] for u in d if u["sub"]=="'"$CUSTOMER"'"][0]')
  VERIFIER=$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')
  CHALLENGE=$(PKCE_VERIFIER="$VERIFIER" python3 -c 'import base64,hashlib,os; print(base64.urlsafe_b64encode(hashlib.sha256(os.environ["PKCE_VERIFIER"].encode()).digest()).rstrip(b"=").decode())')
  curl -s -o "$TMP/authorize.html" --max-time 5 -G "$KEYSTONE_URL/oauth2/v1/authorize" \
    --data-urlencode "client_id=$CLIENT_ID" --data-urlencode "redirect_uri=$REDIRECT_URI" \
    --data-urlencode "response_type=code" --data-urlencode "scope=openid profile email accounts.read" \
    --data-urlencode "state=smoke" --data-urlencode "nonce=n-$RANDOM" \
    --data-urlencode "code_challenge=$CHALLENGE" --data-urlencode "code_challenge_method=S256"
  TXN=$(tr -d '\n' <"$TMP/authorize.html" | sed -n 's/.*name="txn" value="\([^"]*\)".*/\1/p' | head -1)
  if [ -z "$USERNAME" ] || [ -z "$TXN" ]; then
    fail keystone "no login txn for $USERNAME (is keystone-idp-mock up on $KEYSTONE_URL?)"
  else
    curl -s -o /dev/null --max-time 5 -X POST "$KEYSTONE_URL/login" --data-urlencode "txn=$TXN" --data-urlencode "username=$USERNAME" --data-urlencode "password=Passw0rd"
    CODE=$(curl -s -o /dev/null --max-time 5 -w '%{redirect_url}' -X POST "$KEYSTONE_URL/mfa" --data-urlencode "txn=$TXN" --data-urlencode "code=123456" | sed -n 's/.*[?&]code=\([^&]*\).*/\1/p')
    TOKEN=$(curl -s --max-time 5 -X POST "$KEYSTONE_URL/oauth2/v1/token" --data-urlencode "grant_type=authorization_code" \
      --data-urlencode "client_id=$CLIENT_ID" --data-urlencode "code=$CODE" \
      --data-urlencode "code_verifier=$VERIFIER" --data-urlencode "redirect_uri=$REDIRECT_URI" | json 'd["access_token"]')
    if [ -n "$TOKEN" ]; then ok keystone "access token for $USERNAME ($CUSTOMER)"; else fail keystone "token endpoint gave no access_token"; fi
  fi
fi
AUTH=(-H "Authorization: Bearer $TOKEN")

echo "== 2 bedrock-adapter"
RN=$(curl -s --max-time 5 "${AUTH[@]}" "http://localhost:4516/bedrock/v1/accounts/$ACCOUNT" | json 'd["routingNumber"]')
if [ "$RN" = "021000000" ]; then ok bedrock-adapter "ACCT-INQ $ACCOUNT routing 021000000"; else fail bedrock-adapter "inquiry for $ACCOUNT returned routing '$RN'"; fi
MODE=$(curl -s --max-time 5 "${AUTH[@]}" http://localhost:4516/bedrock/v1/status | json 'd["mode"]+"/"+d["gateway"]')
[ -n "$MODE" ] && echo "        gateway $MODE"

echo "== 3 bff-retail"
if [ -z "$TOKEN" ]; then skip bff-retail "no token"; else
  N=$(curl -s --max-time 8 "${AUTH[@]}" http://localhost:4500/api/v1/accounts | json 'len(d if isinstance(d,list) else d.get("accounts", d.get("items", [])))')
  if [ -n "$N" ] && [ "$N" -ge 1 ] 2>/dev/null; then ok bff-retail "/api/v1/accounts returned $N accounts"; else fail bff-retail "/api/v1/accounts returned '$N'"; fi
fi

echo "== 4 beacon ordering"
# Sequences continue from wherever this customer's stream is, so the script can run twice against
# the same beacon without the sequencer treating the second batch as replays (PLAT-1512).
BASE=$(curl -s --max-time 5 "${AUTH[@]}" "http://localhost:4510/beacon/v1/customers/$CUSTOMER/notifications" | json 'max([n.get("sequence",0) for n in d] or [0])')
BASE=${BASE:-0}
curl -s -o /dev/null -X DELETE http://localhost:4510/debug/dispatches
RUN="$(date +%s)"
for ev in \
  '{"seq":3,"eventType":"OVERDRAFT","amountMinor":-4200,"balanceAfterMinor":-1550,"occurredAt":"2024-03-04T15:30:00Z"}' \
  '{"seq":1,"eventType":"LARGE_DEBIT","amountMinor":-900000,"balanceAfterMinor":100,"occurredAt":"2024-03-04T15:00:00Z"}' \
  '{"seq":2,"eventType":"LOW_BALANCE","amountMinor":0,"balanceAfterMinor":100,"occurredAt":"2024-03-04T15:10:00Z"}'; do
  body=$(printf '%s' "$ev" | python3 -c "
import json,sys; d=json.load(sys.stdin); n=d.pop('seq')
d.update(customerId='$CUSTOMER', accountId='$ACCOUNT', eventId='SMK-$RUN-%d' % n, sequence=$BASE+n); print(json.dumps(d))")
  curl -s -o /dev/null --max-time 5 -X POST http://localhost:4510/debug/ingest -H 'Content-Type: application/json' -d "$body"
done
sleep 2
SEQ=$(curl -s --max-time 5 "http://localhost:4510/debug/dispatches?customerId=$CUSTOMER" | json '",".join(str(r["sequence"]-'"$BASE"') for r in d)')
if [ "$SEQ" = "1,2,3" ]; then ok beacon "3 events ingested 3,1,2 processed as $SEQ (from sequence $BASE)"; else fail beacon "processed order '$SEQ' (wanted 1,2,3, base $BASE)"; fi
NOTIFIED=$(curl -s --max-time 5 "http://localhost:4510/debug/dispatches?customerId=$CUSTOMER" | json 'sum(1 for r in d if r["decision"]=="NOTIFY")')
echo "        $NOTIFIED of 3 produced a notification (the rest suppressed by the customer's preferences)"

echo "== 5 audit-trail"
CORR="smoke-$(date +%s)"
CODE=$(curl -s -o "$TMP/audit" --max-time 5 -w '%{http_code}' -X POST http://localhost:4514/audit/v1/events "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sourceService\":\"smoke\",\"eventType\":\"SMOKE_APPEND\",\"subjectType\":\"CUSTOMER\",\"subjectId\":\"$CUSTOMER\",\"actor\":\"smoke\",\"outcome\":\"SUCCESS\",\"correlationId\":\"$CORR\",\"payload\":{\"n\":1}}")
if [ "$CODE" = "201" ]; then
  FOUND=$(curl -s --max-time 5 "${AUTH[@]}" "http://localhost:4514/audit/v1/events?correlationId=$CORR" | json 'len(d)')
  VALID=$(curl -s --max-time 5 "${AUTH[@]}" http://localhost:4514/audit/v1/verify | json 'd.get("valid", d.get("intact"))')
  if [ "$FOUND" = "1" ] && [ "$VALID" = "True" ]; then ok audit-trail "append 201, query by correlation 1 row, chain verifies"; else fail audit-trail "query=$FOUND verify=$VALID"; fi
else
  fail audit-trail "append returned $CODE: $(head -c 160 "$TMP/audit")"
fi

echo "== 6 entitlements"
ANON=$(curl -s -o /dev/null --max-time 5 -w '%{http_code}' http://localhost:4515/entitlements/v1/roles)
if [ "$ANON" = "401" ]; then ok entitlements "anonymous /roles -> 401"; else fail entitlements "anonymous /roles -> $ANON"; fi
if [ -z "$TOKEN" ]; then skip entitlements "no token"; else
  ROLES=$(curl -s --max-time 5 "${AUTH[@]}" http://localhost:4515/entitlements/v1/roles | json 'len(d)')
  if [ -n "$ROLES" ] && [ "$ROLES" -ge 1 ] 2>/dev/null; then ok entitlements "$ROLES roles with token"; else fail entitlements "roles with token returned '$ROLES'"; fi
fi

echo "== 7 documents"
if [ -z "$TOKEN" ]; then skip documents "no token"; else
  URL="http://localhost:4518/api/v1/statements/$ACCOUNT/latest.pdf"
  S1=$(curl -s -D "$TMP/h1" -o "$TMP/p1" --max-time 20 "${AUTH[@]}" "$URL" -w '%{http_code}')
  S2=$(curl -s -D "$TMP/h2" -o "$TMP/p2" --max-time 20 "${AUTH[@]}" "$URL" -w '%{http_code}')
  SRC1=$(grep -i '^x-meridian-source' "$TMP/h1" | tr -d '\r' | awk '{print $2}')
  SRC2=$(grep -i '^x-meridian-source' "$TMP/h2" | tr -d '\r' | awk '{print $2}')
  if [ "$S1" = 200 ] && [ "$S2" = 200 ] && head -c 4 "$TMP/p2" | grep -q '%PDF' && [ "$SRC2" = "archive" ]; then
    ok documents "latest.pdf $(wc -c <"$TMP/p1") bytes, source $SRC1 then $SRC2"
  else
    fail documents "http $S1/$S2 source '$SRC1'/'$SRC2'"
  fi
fi

echo "== 8 statements-api"
PERIOD=$(curl -s --max-time 10 "http://localhost:4519/statements/v1/accounts/$ACCOUNT/periods" | json 'd[0]["period"]')
if [ -z "$PERIOD" ]; then fail statements-api "no periods for $ACCOUNT"; else
  curl -s -o "$TMP/stmt.pdf" --max-time 20 "http://localhost:4519/statements/v1/accounts/$ACCOUNT/$PERIOD.pdf"
  SIZE=$(wc -c <"$TMP/stmt.pdf")
  if head -c 4 "$TMP/stmt.pdf" | grep -q '%PDF' && [ "$SIZE" -gt 1000 ]; then ok statements-api "$PERIOD.pdf is $SIZE bytes"; else fail statements-api "$PERIOD.pdf is $SIZE bytes and not a PDF"; fi
fi

echo
echo "smoke: $PASS passed, $FAIL failed"
exit "$FAIL"
