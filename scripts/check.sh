#!/usr/bin/env bash
# Backend tests + frontend lint + typecheck.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "== 1/3 Backend tests (./gradlew test) =="
(cd "$ROOT/apps/server" && ./gradlew test)

echo "== 2/3 Frontend lint (eslint) =="
(cd "$ROOT/apps/web" && npm run lint)

echo "== 3/3 Frontend typecheck (tsc --noEmit) =="
(cd "$ROOT/apps/web" && npx tsc --noEmit)

echo
echo "✅ All checks passed."
