#!/usr/bin/env bash
# Manage the local PostgreSQL dev database (Docker).
# Usage: db.sh {up|down|status|logs}
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INIT_SCRIPT="$ROOT/apps/server/db-dev-init.sh"
CONTAINER="curtis-postgres-v2"

[ $# -eq 1 ] || { echo "Usage: $0 {up|down|status|logs}"; exit 1; }

case "$1" in
  up)
    if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "✅ PostgreSQL already running ($CONTAINER)."
    else
      "$INIT_SCRIPT"
    fi
    ;;
  down)
    if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      docker stop "$CONTAINER" >/dev/null
      echo "PostgreSQL stopped; its named volume was preserved."
    elif docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "PostgreSQL is already stopped."
    else
      echo "PostgreSQL container does not exist."
    fi
    ;;
  status)
    if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "✅ PostgreSQL running ($CONTAINER)."
    else
      echo "⛔ PostgreSQL not running ($CONTAINER). Start it with: $0 up"
      exit 1
    fi
    ;;
  logs)
    docker logs -f "$CONTAINER"
    ;;
  *)
    echo "Usage: $0 {up|down|status|logs}"
    exit 1
    ;;
esac
