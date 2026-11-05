# ADR-0002: Java 21 and Spring Boot for the backend

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

The core of Plainledger is financial arithmetic: cost bases, pro-rata allocations and PnL that must be exact to the last decimal. The backend also needs authentication, scheduled jobs (price sync), resilient HTTP clients, and solid testing tools. The owner has deep Java and Spring experience and must be able to defend every line.

## Decision

The backend is **Java 21 on Spring Boot 4**, built with Maven (wrapper committed).

## Consequences

- ✅ `BigDecimal` is a first-class, exact decimal type with explicit scale and rounding. It is the default in Java financial code.
- ✅ Mature, well-integrated building blocks: Spring Security, Spring Session, scheduling, Testcontainers, WireMock, jqwik, ArchUnit, Error Prone.
- ✅ Java 21 records and sealed types fit an immutable domain model.
- ⚠️ More boilerplate and slower startup than a Node service. Acceptable for a long-running service.

## Alternatives considered

- **Node.js / TypeScript** — one language across the stack, but no native decimal type: every calculation would go through a library (`decimal.js`), and `number` creeps in easily. The owner's depth is also in Java.
- **Kotlin + Spring** — nicer syntax, but adds a language without changing the trade-offs above.
