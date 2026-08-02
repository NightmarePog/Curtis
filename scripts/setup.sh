#!/usr/bin/env bash
# First-time setup: backend .env + frontend dependencies.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT/scripts/env.sh"

if [ ! -d "$ROOT/apps/web/node_modules" ]; then
  echo "📦 Installing frontend dependencies…"
  (cd "$ROOT/apps/web" && npm install --no-audit --no-fund)
else
  echo "✅ Frontend dependencies already installed."
fi

echo
echo "✅ Setup complete. Start the stack with: make dev"
