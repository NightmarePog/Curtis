#!/usr/bin/env bash
# Start the frontend (Next.js) on http://localhost:3000.
set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib.sh"

demo=false
case "${1:-}" in
  "") ;;
  --demo) demo=true ;;
  *)
    echo "Usage: $0 [--demo]"
    exit 1
    ;;
esac

if port_open 3000; then
  echo "Frontend already running on http://localhost:3000 (pid $(pid_of_port 3000))."
  exit 0
fi

if [ ! -d "$ROOT/apps/web/node_modules" ]; then
  echo "node_modules missing, installing frontend deps…"
  (cd "$ROOT/apps/web" && npm ci --no-audit --no-fund)
fi

echo "Starting frontend…"
if $demo; then
  (cd "$ROOT/apps/web" && NEXT_PUBLIC_DEMO_MODE=true npm run dev) > "$LOG_DIR/web.log" 2>&1 &
else
  (cd "$ROOT/apps/web" && npm run dev) > "$LOG_DIR/web.log" 2>&1 &
fi
write_pid web "$!"

if wait_port 3000 90; then
  echo "Frontend running on http://localhost:3000"
  echo "   Log: $LOG_DIR/web.log"
else
  echo "Frontend failed to start. See $LOG_DIR/web.log"
  exit 1
fi
