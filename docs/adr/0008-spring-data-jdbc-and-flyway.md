# ADR-0008: Spring Data JDBC and Flyway for persistence

- **Status:** Proposed
- **Date:** 2026-11-04

## Context

The persisted model is small: users, sessions, transactions, assets and market prices. Derived state is not stored (ADR-0005). Writes need explicit control: replay validation and a per-(user, asset) lock in the same database transaction (RULE-15).

## Decision

- **Spring Data JDBC** for repositories: aggregates map to tables, each load and save is explicit, and SQL is written by hand where it matters (for example, the advisory lock).
- **Flyway** for schema migrations: versioned SQL files, applied on startup, never edited once merged.
- **PostgreSQL 18** in every environment (Testcontainers in tests, Docker Compose locally), so tests run against the real engine.

## Consequences

- ✅ No persistence context, lazy loading or dirty checking: what you call is what runs, which is easy to reason about and explain.
- ✅ `NUMERIC` maps to `BigDecimal` without surprises.
- ✅ Migrations are plain SQL, reviewable in a PR.
- ⚠️ No automatic change tracking or relationship management. Not needed for these aggregates.

## Alternatives considered

- **Spring Data JPA / Hibernate** — the most common choice. It brings a persistence context, proxies and flush timing that this model does not need and that an interviewer could fairly probe.
- **jOOQ** — excellent type-safe SQL, but adds code generation and a build step for a handful of queries.
- **Liquibase** — equivalent to Flyway. Flyway's plain SQL files are simpler to read.
