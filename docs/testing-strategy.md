# Testing strategy

Most of Plainledger's code is written by an AI agent. Tests are how the owner proves it does what the spec says. The strategy has one goal: **every acceptance criterion is verified by a test that actually passed, at the cheapest level that proves it.**

## Levels

| Level | Tool | What it proves | Example |
|---|---|---|---|
| Domain unit | JUnit 5 | Exact results for specific cases, including the product spec's worked example | Matching T4 yields realized PnL 5,952 |
| Property-based | jqwik | Invariants over thousands of generated histories | Allocated fees always sum to the sell fee; no replay ever oversells; realized + unrealized is independent of the matching order |
| Architecture | ArchUnit | Package and arithmetic rules ([ADR-0003](adr/0003-feature-packages-with-layers.md), [ADR-0006](adr/0006-exact-decimal-arithmetic.md)) | Controllers never use repositories; no `double` in `domain` |
| Web layer | Spring MockMvc | HTTP contract: status codes, Problem Details, validation, JSON decimals as strings | `POST` with `"quantity": 0.1` (a number) → `400` |
| Integration | Testcontainers (PostgreSQL 18) | SQL, migrations, data isolation, locking, against the real database | User B gets `404` for user A's transaction; concurrent oversell is prevented |
| External HTTP | WireMock | Price provider adapter: parsing, timeouts, retries, failure modes | Provider returns 500 three times → prices marked stale, no crash |
| Frontend | Vitest + Testing Library | Components render and behave from the user's point of view | Stale price shows its age |
| Stack smoke | `node:test` against `make up` | The real containers start and talk to each other | `/actuator/health` through Nginx reports the database `UP` |
| End to end | Playwright (one journey, from spec 003) | The critical journey works in a real browser | Register → record buy and sell → see realized PnL |

Rules:
- **Hermetic.** No test calls the internet (NFR-03). External APIs are stubbed with WireMock.
- **Real database.** Integration tests use PostgreSQL in a container, not H2: `NUMERIC`, locking and SQL behavior must be the real ones.
- **Test-first per AC.** The agent writes the AC's test, watches it fail, then implements (`/implement`).
- **No weakening.** Tests, ArchUnit rules and static-analysis checks are never relaxed to get a green build.

## Traceability

Each test declares the ACs it verifies:

```java
@Test
@Verifies("LEDGER-AC-03")
void rejectsSellLargerThanHolding() { ... }
```

```ts
it("[LEDGER-AC-03] shows the oversell error next to the quantity field", ...)
```

After all suites run, `make traceability` (and the CI gate) builds the AC → passing tests matrix from the **test results**, not from source code. It fails when a `done` spec has an uncovered AC, or when a test references an unknown AC. See [ADR-0009](adr/0009-acceptance-criteria-traceability.md).

## Quality gates in CI

| Gate | Fails the build when |
|---|---|
| Spotless (palantir-java-format) | Java code is not formatted |
| Error Prone | Bug patterns are found, including `BigDecimal.equals` and `BigDecimal` built from a double literal |
| oxlint, Prettier, `tsc -b` | Lint errors or warnings, unformatted files, type errors |
| Unit, property, architecture, web, integration tests | Any test fails |
| Stack smoke tests | The composed system does not start or is unhealthy |
| Traceability | An AC of a `done` spec lacks a passing test; a test references an unknown AC |
| CodeQL | Security findings (reported in the Security tab) |

Line coverage (JaCoCo) is collected and published as a signal but is **not** a gate: it measures executed lines, not verified requirements.

## Independent review

Passing tests can still be weak tests. During `/verify`, the read-only `spec-verifier` agent reviews every AC's tests as a skeptic: does the test arrange the AC's *Given*, perform its *When*, and assert its *Then* with exact values? Its findings go into the feature's `verification.md`.
