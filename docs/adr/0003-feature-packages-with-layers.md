# ADR-0003: Package by feature, with layers inside each feature

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

The backend has a handful of features (accounts, ledger, purchases, prices, timeline) and one deployable. We want feature boundaries that are visible and enforced. We also want the conventional layering every Spring developer recognizes. And the calculation logic must be easy to test without Spring.

## Decision

One deployable application (a modular monolith), **packaged by feature**, with classic layers **inside** each feature:

```
com.plainledger
├── <feature>/
│   ├── controller/   HTTP: request/response DTOs, validation, status codes
│   ├── service/      use cases, transactions, user scoping
│   ├── repository/   persistence (Spring Data JDBC)
│   └── domain/       plain Java model and calculations (only where there is real logic)
└── shared/           cross-cutting configuration (errors, JSON, security)
```

Rules, enforced by ArchUnit tests in CI:

1. Layer direction: `controller → service → repository`. Controllers never use repositories; lower layers never use upper ones.
2. `domain` is framework-free: no Spring, no Jakarta, no other layer. This is where property-based tests target the calculation invariants.
3. A feature may use another feature's `service` or `domain`, never its `controller` or `repository`.
4. No binary floating point (`double`/`float`) in `domain`, and no `BigDecimal` built from a `double` anywhere (see ADR-0006).

## Consequences

- ✅ Recognizable layout. A feature's code lives in one place, and deleting a feature is deleting a folder.
- ✅ The engine (replay, matching, PnL) can be tested as pure functions, with no Spring context.
- ✅ Rules are tests, so they cannot erode silently. Agents get immediate feedback when they cross a boundary.
- ⚠️ Four sub-packages for small features add some ceremony. We create `domain/` only where it holds real logic.

## Alternatives considered

- **Layers at the top level** (`controller/`, `service/`, `repository/`) — feature boundaries become invisible, and cross-feature coupling grows unnoticed.
- **Hexagonal / ports and adapters everywhere** — more indirection than four small features need. We keep interfaces only at the real extension points: `MatchingStrategy` and `PriceProvider`.
- **Spring Modulith** — good boundary tooling, but one more framework to learn and defend. ArchUnit covers what we need.
