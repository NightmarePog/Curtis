#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="curtis-postgres-v2"
DATABASE_PORT=5432
DATABASE_VOLUME="curtis_pgdata_v2"
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

if "$CONTAINER_CMD" ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "PostgreSQL is already running in $CONTAINER_NAME."
  exit 0
fi

if "$CONTAINER_CMD" ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "Starting the existing PostgreSQL container…"
  "$CONTAINER_CMD" start "$CONTAINER_NAME" >/dev/null
else
  if ss -ltn 2>/dev/null | grep -qE ":${DATABASE_PORT} "; then
    echo "Port ${DATABASE_PORT} is already in use; PostgreSQL was not started." >&2
    exit 1
  fi

  echo "Creating the PostgreSQL development container…"
  "$CONTAINER_CMD" run -d \
    --name "$CONTAINER_NAME" \
    -e POSTGRES_DB=curtisdb \
    -e POSTGRES_USER=curtisuser \
    -e POSTGRES_PASSWORD=curtispass \
    -p "${DATABASE_PORT}:5432" \
    -v "${DATABASE_VOLUME}:/var/lib/postgresql/data" \
    docker.io/library/postgres:16-alpine >/dev/null
fi

echo "Waiting for PostgreSQL…"
until "$CONTAINER_CMD" exec "$CONTAINER_NAME" pg_isready -U curtisuser -d curtisdb >/dev/null 2>&1; do
  sleep 1
done

echo "PostgreSQL is ready."
