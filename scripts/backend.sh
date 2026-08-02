#!/usr/bin/env bash
# Start the backend (Spring Boot) on http://localhost:3001.
# Requires a running PostgreSQL (scripts/db.sh up).
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

if port_open 3001; then
  echo "ℹ️  Backend already running on http://localhost:3001 (pid $(pid_of_port 3001))."
  exit 0
fi

"$ROOT/scripts/env.sh"

echo "🚀 Starting backend…"
(cd "$ROOT/apps/server" && ./gradlew bootRun --console=plain) > "$LOG_DIR/backend.log" 2>&1 &
write_pid backend "$!"

if wait_port 3001 120; then
  echo "✅ Backend running on http://localhost:3001"
  echo "   OpenAPI: http://localhost:3001/openapi"
  echo "   Log:     $LOG_DIR/backend.log"
else
  echo "❌ Backend failed to start. See $LOG_DIR/backend.log"
  exit 1
fi
