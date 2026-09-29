#!/usr/bin/env bash
# Smoke-test a running production deployment through its public URL.
# Usage: scripts/prod-smoke.sh https://quiz.example.com [-k]
#   -k  accept a self-signed certificate (DOMAIN=localhost testing only)
set -uo pipefail

BASE="${1:?Usage: $0 <https-base-url> [-k]}"
BASE="${BASE%/}"
INSECURE=""
[ "${2:-}" = "-k" ] && INSECURE="-k"
HOST="${BASE#https://}"
CURL=(curl -sS --max-time 15 $INSECURE)
FAILED=0

pass() { echo "✅ $1"; }
fail() { echo "❌ $1"; FAILED=1; }

check_status() {
  local label="$1" path="$2" expected="$3" status
  status="$("${CURL[@]}" -o /dev/null -w '%{http_code}' "$BASE$path" 2>/dev/null)"
  if [ "$status" = "$expected" ]; then pass "$label ($status)"; else fail "$label (expected $expected, got ${status:-no response})"; fi
}

if [ -z "$INSECURE" ]; then
  if curl -sS --max-time 15 -o /dev/null "$BASE/" 2>/dev/null; then pass "TLS certificate is valid"; else fail "TLS certificate is invalid or site unreachable"; fi
fi

location="$("${CURL[@]}" -o /dev/null -w '%{redirect_url}' "http://$HOST/" 2>/dev/null)"
if [[ "$location" == https://* ]]; then pass "HTTP redirects to HTTPS"; else fail "HTTP does not redirect to HTTPS (got '${location}')"; fi

health="$("${CURL[@]}" "$BASE/api/actuator/health/readiness" 2>/dev/null)"
if [[ "$health" == *'"status":"UP"'* ]]; then pass "Backend readiness is UP"; else fail "Backend readiness not UP (got '${health}')"; fi

check_status "Unauthenticated /api/v1/me is rejected" /api/v1/me 401
check_status "OpenAPI is not public" /api/openapi 404
check_status "Actuator internals are not public" /api/actuator/info 404

location="$("${CURL[@]}" -o /dev/null -w '%{redirect_url}' "$BASE/api/oauth2/authorization/microsoft" 2>/dev/null)"
expected_redirect="redirect_uri=$(printf '%s' "$BASE/api/login/oauth2/code/microsoft" | sed 's/:/%3A/g; s#/#%2F#g')"
if [[ "$location" == https://login.microsoftonline.com/* ]]; then
  pass "Login redirects to Microsoft"
  if [[ "$location" == *"$expected_redirect"* ]]; then pass "OAuth redirect_uri matches $BASE"; else fail "OAuth redirect_uri does not match $BASE ($location)"; fi
else
  fail "Login does not redirect to Microsoft (got '${location}')"
fi

headers="$("${CURL[@]}" -o /dev/null -D - "$BASE/" 2>/dev/null | tr -d '\r')"
status="$(printf '%s\n' "$headers" | head -1 | awk '{print $2}')"
if [[ "$status" =~ ^[23] ]]; then pass "Frontend responds ($status)"; else fail "Frontend status ${status:-none}"; fi
grep -qi '^strict-transport-security:' <<<"$headers" && pass "HSTS header present" || fail "HSTS header missing"
grep -qi '^x-content-type-options: nosniff' <<<"$headers" && pass "nosniff header present" || fail "nosniff header missing"

echo
if [ "$FAILED" -eq 0 ]; then echo "✅ All production smoke checks passed."; else echo "❌ Some checks failed."; exit 1; fi
