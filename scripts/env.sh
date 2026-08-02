#!/usr/bin/env bash
# Ensure apps/server/.env exists (copied from .env.example).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
EXAMPLE="$ROOT/apps/server/.env.example"
ENV_FILE="$ROOT/apps/server/.env"

if [ ! -f "$ENV_FILE" ]; then
  cp "$EXAMPLE" "$ENV_FILE"
  echo "📝 Created $ENV_FILE from the template."
  echo "   Fill in CLIENT_SECRET if you have the real Entra ID secret."
else
  echo "✅ $ENV_FILE already exists."
fi
