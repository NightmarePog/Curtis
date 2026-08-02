.PHONY: help dev backend web db-up db-down db-status db-logs setup check build stop stop-all

help:
	@echo "Curtis – dev helpers"
	@echo ""
	@echo "  make setup      First-time setup (.env + npm install)"
	@echo "  make dev        Start PostgreSQL + backend + frontend (Ctrl+C stops everything)"
	@echo "  make backend    Start only the backend (http://localhost:3001)"
	@echo "  make web        Start only the frontend (http://localhost:3000)"
	@echo "  make db-up      Start the PostgreSQL container"
	@echo "  make db-down    Stop and remove the PostgreSQL container"
	@echo "  make db-status  PostgreSQL status"
	@echo "  make db-logs    Follow PostgreSQL logs"
	@echo "  make check      Backend tests + frontend lint + typecheck"
	@echo "  make build      Production build of the frontend"
	@echo "  make stop       Stop the dev servers"
	@echo "  make stop-all   Stop the dev servers and the database"

dev:
	scripts/dev.sh

backend:
	scripts/backend.sh

web:
	scripts/web.sh

db-up:
	scripts/db.sh up

db-down:
	scripts/db.sh down

db-status:
	scripts/db.sh status

db-logs:
	scripts/db.sh logs

setup:
	scripts/setup.sh

check:
	scripts/check.sh

build:
	scripts/build.sh

stop:
	scripts/stop.sh

stop-all:
	scripts/stop.sh --all
