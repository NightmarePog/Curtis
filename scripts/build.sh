#!/usr/bin/env bash
# Production build of the frontend.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
(cd "$ROOT/apps/web" && npm run build)
