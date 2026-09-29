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

# Export local development values for Spring Boot. Production uses real
# environment variables and never reads this file.
set -a
# shellcheck disable=SC1091
source "$ROOT/apps/server/.env"
set +a

# The container deployment uses /data, but the local JVM runs directly on the
# host and needs writable paths. Keep existing user-provided values unchanged.
export QUIZ_IMPORT_ROOT="${QUIZ_IMPORT_ROOT:-/tmp/curtis-import}"
export MEDIA_ROOT="${MEDIA_ROOT:-/tmp/curtis-media}"
mkdir -p "$QUIZ_IMPORT_ROOT" "$MEDIA_ROOT"

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
