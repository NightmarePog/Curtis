set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

# Docker Compose on servers, podman-compose where Docker is not installed.
compose := `command -v docker >/dev/null 2>&1 && echo "docker compose" || echo "podman-compose"`

default:
    @just --list

# Start PostgreSQL, the backend, and the frontend.
dev:
    scripts/dev.sh

# Start the frontend with deterministic mock data; no backend is required.
demo:
    scripts/env.sh
    scripts/web.sh --demo

# Start only the backend.
backend:
    scripts/backend.sh

# Start only the frontend.
web:
    scripts/web.sh

# Start the local PostgreSQL container.
db-up:
    scripts/db.sh up

# Stop PostgreSQL and preserve its volume.
db-down:
    scripts/db.sh down

# Show PostgreSQL status.
db-status:
    scripts/db.sh status

# Follow PostgreSQL logs.
db-logs:
    scripts/db.sh logs

# Create local environment files and install locked frontend dependencies.
setup:
    scripts/setup.sh

# Run backend tests, frontend lint, and frontend typechecking.
check: test lint typecheck
    @echo
    @echo "✅ All checks passed."

# Run backend tests.
test:
    cd apps/server && ./gradlew test

# Run frontend lint.
lint:
    cd apps/web && npm run lint

# Run frontend TypeScript checks.
typecheck:
    cd apps/web && npm run typecheck

# Build backend and frontend for production.
build: build-backend build-web

# Build the backend for production.
build-backend:
    cd apps/server && ./gradlew build

# Build the frontend for production.
build-web:
    cd apps/web && npm run build

# Stop the dev servers.
stop:
    scripts/stop.sh

# Stop the dev servers and PostgreSQL.
stop-all:
    scripts/stop.sh --all

# Build and start the production stack (configure .env first).
prod-up:
    {{compose}} up -d --build

# Run the production stack locally on http://localhost:8080 (no TLS).
prod-local:
    HTTP_PORT=8080 HTTPS_PORT=8443 {{compose}} -f docker-compose.yml -f docker-compose.local.yml up -d --build

# Stop the production stack and preserve its volumes.
prod-down:
    {{compose}} down

# Show production container status.
prod-ps:
    {{compose}} ps

# Follow production logs.
prod-logs:
    {{compose}} logs -f --tail=200

# Smoke-test a deployment, e.g. `just prod-smoke https://quiz.example.com`.
prod-smoke url *flags:
    scripts/prod-smoke.sh {{url}} {{flags}}

# Dump the production database to backups/.
prod-backup:
    scripts/prod-backup.sh
