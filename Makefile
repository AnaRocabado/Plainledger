# Plainledger: one entry point for humans, CI and AI agents. Run `make` to list the targets.
.DEFAULT_GOAL := help
.PHONY: help up down reset logs dev-db verify backend-verify frontend-verify tools-test traceability smoke fmt clean

COMPOSE := docker compose
WEB_URL := http://localhost:$(or $(WEB_PORT),3000)
# node:test prints readable output and writes JUnit XML (traceability evidence, ADR-0009).
NODE_TEST = node --test --test-reporter=spec --test-reporter-destination=stdout --test-reporter=junit

help: ## Show this help
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ---- Run the system --------------------------------------------------------------------------

up: ## Build and start everything; returns once the app is healthy
	$(COMPOSE) up --build --detach --wait
	@echo "Plainledger is running at $(WEB_URL)"

down: ## Stop all services (data is kept)
	$(COMPOSE) down

reset: ## Stop all services and delete the database volume
	$(COMPOSE) down --volumes

logs: ## Follow the logs of all services
	$(COMPOSE) logs --follow

dev-db: ## Start only PostgreSQL on localhost:5432, to run the backend from an IDE
	$(COMPOSE) -f docker-compose.yml -f docker-compose.dev.yml up --detach --wait db

# ---- Verify ----------------------------------------------------------------------------------

verify: backend-verify frontend-verify tools-test traceability ## Everything CI runs, except image builds and smoke tests

backend-verify: ## Backend: format check, Error Prone, unit and integration tests (needs Docker)
	cd backend && ./mvnw -B verify

frontend/node_modules: frontend/package-lock.json
	cd frontend && npm ci
	@touch $@

frontend-verify: frontend/node_modules ## Frontend: lint, typecheck, format check, tests
	cd frontend && npm run check

tools-test: ## Tests of the traceability tool
	@mkdir -p tools/traceability/test-results
	$(NODE_TEST) --test-reporter-destination=tools/traceability/test-results/junit.xml "tools/traceability/test/*.test.mjs"

traceability: ## Acceptance criteria -> passing tests matrix, from the latest test results
	node tools/traceability/check.mjs --report build/traceability.md

smoke: ## Smoke-test the running stack (run `make up` first)
	@mkdir -p tools/smoke/test-results
	BASE_URL=$(WEB_URL) $(NODE_TEST) --test-reporter-destination=tools/smoke/test-results/junit.xml "tools/smoke/*.test.mjs"

# ---- Maintenance -----------------------------------------------------------------------------

fmt: frontend/node_modules ## Apply formatters (Spotless, Prettier)
	cd backend && ./mvnw -B -q spotless:apply
	cd frontend && npm run format

clean: ## Delete build output and test results
	cd backend && ./mvnw -B -q clean
	rm -rf build frontend/dist frontend/test-results tools/traceability/test-results tools/smoke/test-results
