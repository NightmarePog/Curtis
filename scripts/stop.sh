#!/usr/bin/env bash
# Stop the dev servers (backend + frontend). Use --all to also stop PostgreSQL.
set -u

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

for name in web backend; do
  if is_running "$name"; then
    kill "$(cat "$RUN_DIR/$name.pid")" 2>/dev/null || true
  fi
done

kill_port 3000
kill_port 3001
sleep 1
kill_port 3000
kill_port 3001

rm_pid web
rm_pid backend

echo "⏹  Backend and frontend stopped."

if [ "${1:-}" = "--all" ]; then
  "$ROOT/scripts/db.sh" down
fi
