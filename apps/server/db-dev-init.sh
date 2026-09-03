#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="curtis-postgres-v2"
DATABASE_PORT=5432
DATABASE_VOLUME="curtis_pgdata_v2"

if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "PostgreSQL is already running in $CONTAINER_NAME."
  exit 0
fi

if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  echo "Starting the existing PostgreSQL container…"
  docker start "$CONTAINER_NAME" >/dev/null
else
  if ss -ltn 2>/dev/null | grep -qE ":${DATABASE_PORT} "; then
    echo "Port ${DATABASE_PORT} is already in use; PostgreSQL was not started." >&2
    exit 1
  fi

  echo "Creating the PostgreSQL development container…"
  docker run -d \
    --name "$CONTAINER_NAME" \
    -e POSTGRES_DB=curtisdb \
    -e POSTGRES_USER=curtisuser \
    -e POSTGRES_PASSWORD=curtispass \
    -p "${DATABASE_PORT}:5432" \
    -v "${DATABASE_VOLUME}:/var/lib/postgresql/data" \
    postgres:16-alpine >/dev/null
fi

echo "Waiting for PostgreSQL…"
until docker exec "$CONTAINER_NAME" pg_isready -U curtisuser -d curtisdb >/dev/null 2>&1; do
  sleep 1
done

echo "PostgreSQL is ready."
