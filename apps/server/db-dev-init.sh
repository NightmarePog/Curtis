#!/usr/bin/env bash

set -e

CONTAINER_NAME="curtis-postgres"
PORT=5432

echo "🔍 Checking existing container..."

# Stop + remove container if exists
if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
  echo "🗑 Removing existing container..."
  docker rm -f $CONTAINER_NAME
fi

echo "🔍 Checking processes on port $PORT..."

# Kill process using port
if lsof -i :$PORT >/dev/null 2>&1; then
  echo "⚠️ Port $PORT is in use. Killing process..."
  lsof -ti :$PORT | xargs -r kill -9
fi

echo "🚀 Starting PostgreSQL container..."

docker run -d \
  --name $CONTAINER_NAME \
  -e POSTGRES_DB=curtisdb \
  -e POSTGRES_USER=curtisuser \
  -e POSTGRES_PASSWORD=curtispass \
  -p $PORT:5432 \
  -v pgdata:/var/lib/postgresql/data \
  docker.io/library/postgres:17

echo "⏳ Waiting for database to be ready..."

# Wait loop
until docker exec $CONTAINER_NAME pg_isready -U curtisuser >/dev/null 2>&1; do
  echo "💤 sleeping..."
  sleep 1
done

echo "✅ Database is ready!"
