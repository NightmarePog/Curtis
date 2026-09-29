#!/usr/bin/env bash
# Manage the local PostgreSQL dev database (Docker).
# Usage: db.sh {up|down|status|logs}
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INIT_SCRIPT="$ROOT/apps/server/db-dev-init.sh"
CONTAINER="curtis-postgres-v2"
CONTAINER_CMD="${CONTAINER_CMD:-}"

if [ -z "$CONTAINER_CMD" ]; then
  if command -v docker >/dev/null 2>&1; then
    CONTAINER_CMD="docker"
  elif command -v podman >/dev/null 2>&1; then
    CONTAINER_CMD="podman"
  else
    echo "No container runtime found. Install Docker or Podman, or set CONTAINER_CMD." >&2
    exit 1
  fi
fi

[ $# -eq 1 ] || { echo "Usage: $0 {up|down|status|logs}"; exit 1; }

case "$1" in
  up)
    if "$CONTAINER_CMD" ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "✅ PostgreSQL already running ($CONTAINER)."
    else
      "$INIT_SCRIPT"
    fi
    ;;
  down)
    if "$CONTAINER_CMD" ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      "$CONTAINER_CMD" stop "$CONTAINER" >/dev/null
      echo "PostgreSQL stopped; its named volume was preserved."
    elif "$CONTAINER_CMD" ps -a --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "PostgreSQL is already stopped."
    else
      echo "PostgreSQL container does not exist."
    fi
    ;;
  status)
    if "$CONTAINER_CMD" ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
      echo "✅ PostgreSQL running ($CONTAINER)."
    else
      echo "⛔ PostgreSQL not running ($CONTAINER). Start it with: $0 up"
      exit 1
    fi
    ;;
  logs)
    "$CONTAINER_CMD" logs -f "$CONTAINER"
    ;;
  *)
    echo "Usage: $0 {up|down|status|logs}"
    exit 1
    ;;
esac
