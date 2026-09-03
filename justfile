set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

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
