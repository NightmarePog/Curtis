#!/usr/bin/env bash
# Start the whole dev stack: env -> PostgreSQL -> backend -> frontend.
# Press Ctrl+C to stop everything (including the database).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT/scripts/env.sh"
"$ROOT/scripts/db.sh" up
"$ROOT/scripts/backend.sh"
"$ROOT/scripts/web.sh"

echo
echo "🎉 Dev stack is up:"
echo "   App + API: http://localhost:3000   (/api proxies to the backend)"
echo "   Backend:   http://localhost:3001   (direct diagnostics; OpenAPI: /openapi)"
echo "   Logs:     $ROOT/.logs/backend.log | $ROOT/.logs/web.log"
echo
echo "Press Ctrl+C to stop everything (servers + database)."

trap '"$ROOT/scripts/stop.sh" --all; exit 0' INT TERM

while true; do
  sleep 60
done
