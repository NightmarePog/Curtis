#!/usr/bin/env bash
# Dump the production PostgreSQL database to backups/curtis-<timestamp>.dump.
# Restore with: docker compose exec -T db pg_restore -U <user> -d <db> --clean < file.dump
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$ROOT/backups"
FILE="$ROOT/backups/curtis-$(date +%Y%m%d-%H%M%S).dump"

cd "$ROOT"
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$FILE"
echo "✅ Backup written to $FILE ($(du -h "$FILE" | cut -f1))"
