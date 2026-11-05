---
id: "001"
title: Foundations
prefix: FOUND
status: in-progress
---

# 001 — Foundations

## Context

Before any feature, the project needs a working base: one command to run the whole system, guardrails that keep AI-generated code within the architecture and the decimal policy, a consistent API error and number format, and a CI pipeline that proves every acceptance criterion with a passing test.

Builds on: NFR-01 (one command), NFR-03 (hermetic tests), NFR-04 (traceable quality), RULE-13 (decimal policy), ADR-0003, ADR-0006, ADR-0009.

## Scope

**In scope**
- Backend and frontend skeletons with no business features.
- Docker Compose environment started by `make up`.
- API conventions shared by all features: Problem Details errors, decimals as JSON strings.
- Architecture and arithmetic guardrails as tests.
- The traceability check and the CI pipeline.

**Out of scope**
- Authentication (spec 002); any database table other than Flyway's own.
- Image publishing to any registry other than GitHub Container Registry; deployment to a cloud.

## Requirements

- **FR-1** — `make up` builds and starts the database, backend and web app; `make down` stops them; `make reset` also deletes the data.
- **FR-2** — The system exposes a health endpoint, reachable through the web app's origin, that includes the database's status.
- **FR-3** — API errors use RFC 9457 Problem Details (`application/problem+json`) and never include stack traces.
- **FR-4** — The package rules of ADR-0003 and the arithmetic rules of ADR-0006 are automated tests that fail the build on violation.
- **FR-5** — Decimal values cross the API as JSON strings in plain notation, in both directions (ADR-0006).
- **FR-6** — The web app shows an application shell with the product name and its guiding principle.
- **FR-7** — A traceability check maps every AC to the tests that verify it, from test results, and fails as described in ADR-0009.
- **FR-8** — CI runs on every push and pull request: backend build and tests, frontend checks and tests, tool tests, a smoke test of the composed system, the traceability gate, and CodeQL. Images are pushed to GHCR from `main` only.
- **FR-9** — Static analysis fails the build: Spotless and Error Prone (backend); oxlint, Prettier and the TypeScript compiler (frontend).
- **NFR-1** — Containers run as non-root users. Base images exist for `amd64` and `arm64`.
- **NFR-2** — No secrets in the repository. Local default credentials are documented as local-only and overridable by environment variables.

FR-8 and FR-9 are verified by the CI pipeline itself (a red pipeline is the evidence), not by an AC.

## Acceptance criteria

- **FOUND-AC-01** — Given a machine with Docker, when `make up` completes, then `http://localhost:3000/` returns `200` with the web app's HTML, and a deep link such as `/portfolio` returns the same app (client-side routing). *(FR-1, FR-6)*
- **FOUND-AC-02** — Given the system is up, when a client requests `/actuator/health` on the web app's origin, then it receives `200` with `"status": "UP"` and a `db` component that is `UP`. *(FR-2)*
- **FOUND-AC-03** — Given the architecture test suite, when it inspects code where a controller uses a repository, or a feature uses another feature's repository or controller, then it reports a violation; code that follows the rules passes. *(FR-4)*
- **FOUND-AC-04** — Given the architecture test suite, when it inspects a `domain` class that depends on Spring, a `domain` class with a `double` or `float` field, parameter or return type, or any code that builds a `BigDecimal` from a `double`, then it reports a violation. *(FR-4)*
- **FOUND-AC-05** — Given the API, when a client requests an unknown path under `/api`, then it receives `404` with content type `application/problem+json`, a `status` of `404`, and no stack trace. *(FR-3)*
- **FOUND-AC-06** — Given an API response containing decimals, when it is serialized, then each decimal is a JSON string in plain notation without trailing zeros: `1E-8` → `"0.00000001"`, `40000.000` → `"40000"`, `0` → `"0"`, `-12.50` → `"-12.5"`. *(FR-5)*
- **FOUND-AC-07** — Given an API request with a decimal field, when the field is a JSON string in plain notation (`"0.1"`), then it is accepted exactly; when it is a JSON number (`0.1`) or uses exponent notation (`"1e-8"`), then the request is rejected with `400` Problem Details. *(FR-5, FR-3)*
- **FOUND-AC-08** — Given the web app, when it loads, then it shows the product name "Plainledger" and the guiding principle "Show the real numbers, not the flattering ones." *(FR-6)*
- **FOUND-AC-09** — Given specs and test results, when the traceability check runs, then it fails if an AC of a `done` spec has no passing test, if a test references an unknown AC, if an AC ID is duplicated or does not match its spec's prefix, or if a test referencing an AC failed. It succeeds and prints the AC → tests matrix otherwise, and reports ACs of specs that are not `done` without failing. *(FR-7)*

## Open questions

None.

## Changelog

| Date | Change |
|---|---|
| 2026-11-04 | Draft |
| 2026-11-04 | In progress: implemented under the owner's Phase 1 go-ahead; owner review of the spec pending |
| 2026-11-04 | FR-9: oxlint instead of ESLint, following the current official Vite template |
