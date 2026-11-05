# CLAUDE.md

Project rules for AI agents working in this repository. Humans: see [README.md](README.md) and [specs/README.md](specs/README.md).

## What this is

Plainledger is a multi-user crypto investment **tracker**. It records buys and sells and derives honest numbers from them: what each purchase really cost, what was actually gained on what was sold, and where the portfolio stands today.

**Guiding principle: show the real numbers, not the flattering ones.** When a rule makes the portfolio look worse but more truthful, take it. If a requirement is ambiguous, choose the more conservative interpretation and flag it.

## Sources of truth (in this order)

1. `specs/000-product/spec.md` — product scope, domain rules (`RULE-NN`), formulas, glossary.
2. `specs/NNN-<feature>/spec.md` — feature requirements and acceptance criteria (`<PREFIX>-AC-NN`).
3. `docs/adr/` — architecture decisions. Do not contradict an `Accepted` ADR; propose a new one that supersedes it.
4. The code. If code and spec disagree, the spec wins; fix the code or raise the discrepancy. Never silently "fix" a spec to match code.

## Spec-Driven Development workflow

Every feature goes through these gates. **Stop at each gate and wait for the owner.** Do not skip ahead.

| Step | Skill | Output | Gate |
|---|---|---|---|
| 1. Specify | `/spec` | `spec.md` (status `draft`) | Owner reviews and sets status `approved` |
| 2. Plan | `/plan` | `plan.md` | Owner approves the plan |
| 3. Break down | `/tasks` | `tasks.md` | Owner approves the tasks |
| 4. Implement | `/implement` | Code + tests, one commit per task | Checks green per task |
| 5. Verify | `/verify` | `verification.md`, status `done` | Owner accepts |

Rules:
- Never write production code for a spec that is not `approved`.
- Do not add behavior that no acceptance criterion asks for. If something is missing from the spec, stop and propose a spec change.
- Every acceptance criterion must be verified by at least one **passing** automated test that references its ID (see Testing). CI enforces this for specs with status `done`.
- Decisions with trade-offs that outlive a single feature get an ADR (`/adr`). Ask the owner before any decision that changes scope or architecture.

## Repository map

```
backend/        Spring Boot 4, Java 21, Maven wrapper (./mvnw)
frontend/       React + TypeScript (Vite); served by Nginx in Docker
specs/          000-product + one folder per feature: spec.md, plan.md, tasks.md, verification.md
docs/adr/       Architecture Decision Records
docs/           architecture.md, testing-strategy.md
tools/          traceability check (AC -> passing tests), smoke tests
.claude/        skills for each SDD step, verifier agent, hooks
```

## Commands

```bash
make up              # build and start everything (db, backend, web) at http://localhost:3000
make down            # stop (make reset also deletes the database volume)
make verify          # everything CI runs, except image builds
make traceability    # AC coverage report from the latest test results
make fmt             # apply formatters (Spotless, Prettier)

cd backend && ./mvnw verify            # format check, Error Prone, unit + integration tests
cd frontend && npm run check           # lint, typecheck, format check, tests
```

Integration tests and `make up` need a running Docker daemon.

## Backend conventions

- **Package by feature, layers inside each feature** (ADR-0003):
  `com.plainledger.<feature>.{controller,service,repository,domain}`; cross-cutting code lives in `com.plainledger.shared`.
  - `controller` → `service` → `repository`. Controllers never touch repositories.
  - `domain` is plain Java: no Spring, no Jakarta, no persistence. Calculations live here so they can be property-tested.
  - A feature may use another feature's `service` or `domain`, never its `controller` or `repository`.
  - These rules are enforced by ArchUnit (`ArchitectureTest`). Do not weaken a rule to make a build pass.
- **Money and quantities** (ADR-0006):
  - `BigDecimal` only. Never `double`/`float`. Never `new BigDecimal(double)` or `BigDecimal.valueOf(double)`.
  - Compare with `compareTo`/`signum`, never `equals` (Error Prone fails the build).
  - Division only through the shared decimal policy (scale 18, `HALF_EVEN`). Allocations must sum exactly to the total (remainder goes to the last share).
  - Decimals travel as JSON **strings** in plain notation; requests with JSON numbers for decimal fields are rejected.
- **Derived state is computed, never stored** (ADR-0004, ADR-0005). The transaction ledger is the only persisted financial state.
- **Data isolation**: every query is scoped to the authenticated user. Another user's resource is reported as `404`, not `403`.
- Errors are RFC 9457 Problem Details (`application/problem+json`).
- Persistence: Spring Data JDBC; schema changes only through new Flyway migrations (`V<n>__<description>.sql`). Never edit an applied migration.

## Frontend conventions

- TypeScript strict mode. No arithmetic on money in the browser: treat decimal strings as opaque and only format them for display.
- Server state through the API client; components stay presentational where possible.

## Testing

- Reference acceptance criteria explicitly:
  - Java: `@Verifies("LEDGER-AC-03")` on the test method or class (`com.plainledger.testsupport.Verifies`).
  - TypeScript / Node: put the ID in the test name: `it("[LEDGER-AC-03] rejects a sell larger than the holding", ...)`.
- Naming: unit tests `*Test.java` (Surefire), Testcontainers tests `*IT.java` (Failsafe).
- Calculation invariants get property-based tests (jqwik), e.g. "allocated fees always sum to the sell fee".
- External HTTP APIs are stubbed with WireMock. Tests never call the real internet.
- Write the test for an AC before or together with its implementation, and watch it fail first.

## Commits

- Conventional Commits, imperative mood, English: `feat(ledger): reject sells that exceed the holding`.
- Scopes: a feature (`accounts`, `ledger`, `purchases`, `prices`, `timeline`), `spec-NNN`, `adr`, `backend`, `frontend`, `ci`, `build`, `tools`.
- One task from `tasks.md` per commit; the body references the AC IDs it covers.
- Never commit secrets, `.env` files, or generated build output.

## Definition of done (per feature)

- All ACs covered by passing tests; `make traceability` reports no gaps.
- `make verify` green; CI green.
- `verification.md` written, including the independent review by the `spec-verifier` agent.
- Docs updated: spec status, ADRs, architecture diagram if the shape changed.
