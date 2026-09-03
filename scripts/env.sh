#!/usr/bin/env bash
# Ensure local backend and frontend environment files exist.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_EXAMPLE="$ROOT/apps/server/.env.example"
SERVER_ENV_FILE="$ROOT/apps/server/.env"
WEB_EXAMPLE="$ROOT/apps/web/.env.example"
WEB_ENV_FILE="$ROOT/apps/web/.env.local"

if [ ! -f "$SERVER_ENV_FILE" ]; then
  cp "$SERVER_EXAMPLE" "$SERVER_ENV_FILE"
  echo "Created $SERVER_ENV_FILE from the template."
  echo "Fill in MICROSOFT_CLIENT_SECRET before using real sign-in."
else
  echo "$SERVER_ENV_FILE already exists."
fi

if [ ! -f "$WEB_ENV_FILE" ]; then
  cp "$WEB_EXAMPLE" "$WEB_ENV_FILE"
  echo "Created $WEB_ENV_FILE from the template."
else
  echo "$WEB_ENV_FILE already exists."
fi
