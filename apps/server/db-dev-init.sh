#!/usr/bin/env bash

set -e

CONTAINER_NAME="curtis-postgres"
PORT=5432

echo "🔍 Checking existing container..."

# Stop + remove container if exists
if docker ps -aq -f name="^/${CONTAINER_NAME}$" | grep -q .; then
  echo "🗑 Removing existing container..."
  docker rm -f $CONTAINER_NAME
fi

echo "🔍 Checking processes on port $PORT..."

# Kill anything using the port
PIDS=$(lsof -ti :$PORT || true)

if [ -n "$PIDS" ]; then
  echo "⚠️ Port $PORT is in use. Killing processes: $PIDS"
  kill -9 $PIDS || true
fi

# Try to stop system postgres (very common culprit)
if systemctl is-active --quiet postgresql 2>/dev/null; then
  echo "⚠️ Stopping system PostgreSQL..."
  sudo systemctl stop postgresql
fi

# Final check
if lsof -i :$PORT >/dev/null 2>&1; then
  echo "❌ Port $PORT is STILL in use. Aborting."
  lsof -i :$PORT
  exit 1
fi

echo "🚀 Starting PostgreSQL container..."

docker run -d \
  --name $CONTAINER_NAME \
  -e POSTGRES_DB=curtisdb \
  -e POSTGRES_USER=curtisuser \
  -e POSTGRES_PASSWORD=curtispass \
  -p $PORT:5432 \
  -v pgdata:/var/lib/postgresql/data \
  postgres:17

echo "⏳ Waiting for database to be ready..."

until docker exec $CONTAINER_NAME pg_isready -U curtisuser >/dev/null 2>&1; do
  echo "💤 sleeping..."
  sleep 1
done

echo "✅ Database is ready!"
