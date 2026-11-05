# 001 — Technical plan

Spec: [spec.md](spec.md)

## Approach

Create the smallest skeleton that makes every foundations AC testable, with no business code. The backend and frontend are generated from their official starters (Spring Initializr, Vite) and then tightened: static analysis, decimal JSON policy, Problem Details, architecture tests, and the traceability listener. Docker Compose wires the three services. CI runs everything a developer can run locally, plus a smoke test of the real containers.

## Design

### Repository layout

```
backend/            Maven project, com.plainledger
frontend/           Vite React TypeScript app
tools/traceability  check.mjs (no dependencies) + node:test tests
tools/smoke         node:test smoke tests against a running stack
docker-compose.yml  db (postgres:18-alpine), backend, web (nginx-unprivileged)
Makefile            up, down, reset, logs, verify, traceability, smoke, fmt
```

### Backend

- **Shared configuration** in `com.plainledger.shared`:
  - `shared.json.DecimalJsonModule`: Jackson module with a serializer (`stripTrailingZeros().toPlainString()`, written as a string) and a strict deserializer (accepts only JSON strings matching `-?\d+(\.\d+)?`). Registered globally as a bean.
  - Problem Details enabled with `spring.mvc.problemdetails.enabled=true`. Unreadable request bodies (such as the strict decimal failure) become `400` Problem Details through the framework's default handling.
- **Health**: Actuator, with only `health` exposed. `show-components=always` exposes component statuses (not details), so `db` is visible.
- **Flyway** runs on startup. There are no migrations yet; spec 002 adds the first one.
- **Static analysis**:
  - Spotless (palantir-java-format), checked during `verify`.
  - Error Prone on every compilation, with `BigDecimalEquals` and `BigDecimalLiteralDouble` raised to errors.
- **Tests**:
  - Surefire runs `*Test`; Failsafe runs `*IT` (Testcontainers, PostgreSQL 18).
  - JaCoCo report, without a threshold.
  - `testsupport.Verifies`: a repeatable annotation for test methods or classes.
  - `testsupport.TraceabilityListener`: a JUnit Platform `TestExecutionListener` registered through `ServiceLoader`. It writes `{test, status, criteria}` for every executed test to `target/traceability/<suite>.json`. The suite name comes from a system property set per plugin, and the listener does nothing without it (so IDE runs leave no partial files).
  - `architecture.ArchitectureRules` holds the rules; `ArchitectureTest` applies them to production code **and** to fixture packages under `src/test`, so each rule is proven to catch its violation, even while the codebase is still empty.

### Frontend

- Vite + React + TypeScript strict. Vitest + Testing Library, JUnit reporter to `test-results/junit.xml`.
- ESLint (the Vite template's config), Prettier, `tsc --noEmit`.
- `npm run check` runs all of them.

### Containers

- `backend/Dockerfile`: multi-stage. A JDK image runs `./mvnw package` without tests (tests run in CI, not in image builds). Spring Boot's layered extraction feeds a JRE Alpine runtime with a non-root user. The healthcheck uses `wget` on `/actuator/health`.
- `frontend/Dockerfile`: a Node image builds the app; the runtime is `nginx-unprivileged` (non-root, port 8080). Nginx serves the SPA with a fallback to `index.html`, proxies `/api/` and exactly `/actuator/health` to the backend, and sets basic security headers.
- `docker-compose.yml`: `web` is published on `localhost:3000`. `backend` and `db` are not published. Each service has a healthcheck, and `make up` uses `docker compose up --build --wait`.
- `docker-compose.dev.yml` publishes Postgres on `localhost:5432`, for running the backend from an IDE (`make dev-db`).

### Traceability check

`tools/traceability/check.mjs`, with no dependencies.

**Input**
- Specs: `specs/*/spec.md`, with front matter (`prefix`, `status`) and AC definitions (list items starting with `**<PREFIX>-AC-NN**`).
- Evidence:
  - files with the `plainledger-evidence/v1` JSON format (backend listener),
  - JUnit XML (Vitest, `node:test`, Playwright), with AC IDs extracted from `[ID]` in test names.

**Output**
- Markdown matrix on stdout and to `--report`, also appended to `$GITHUB_STEP_SUMMARY` when set.
- Exit code 1 on any error listed in FOUND-AC-09.

## Decisions

| Decision | Alternatives considered | Why |
|---|---|---|
| Decimal JSON via a global Jackson module | `@JsonFormat` on every field; DTO types holding strings | Global is impossible to forget. Field annotations depend on discipline; string DTOs move parsing into every controller. |
| Strict deserializer rejects JSON numbers | Accept numbers and convert | Accepting `0.1` as a number means trusting the client's float parsing. Rejecting makes the contract explicit (ADR-0006). |
| Traceability evidence from a JUnit Platform listener | Surefire XML + display names containing IDs | The listener sees all engines (Jupiter, jqwik), reads the annotation directly, and keeps test names readable. |
| JUnit XML for JS runners | A custom reporter per runner | Vitest, `node:test` and Playwright all emit JUnit XML natively: one parser instead of three reporters. |
| ArchUnit fixtures under `src/test` | Trust the rules until code exists | Rules that check empty packages pass trivially. Fixtures prove that each rule fails on its violation. |
| `--wait` with healthchecks in Compose | Sleep-and-retry in the Makefile | Compose knows when each service is healthy; `make up` returns only when the app is usable. |
| Smoke tests in CI against the real compose stack | Only unit and integration tests | FOUND-AC-01/02 are about the assembled system. Only the real containers prove them. |
| Tests skipped in the backend image build | Run tests in the Dockerfile | Tests run once, in CI, with Docker available for Testcontainers. Image builds stay fast. |

New ADRs needed: none (ADR-0003, 0006, 0009 cover these decisions).

## Test strategy

| AC | Level | Test |
|---|---|---|
| FOUND-AC-01 | Stack smoke | `tools/smoke/smoke.test.mjs`: root and deep link serve the SPA |
| FOUND-AC-02 | Stack smoke + integration | `smoke.test.mjs`: health through Nginx; `HealthIT` (Testcontainers) |
| FOUND-AC-03 | Architecture | `ArchitectureTest`: layering, feature isolation, compliant fixture |
| FOUND-AC-04 | Architecture | `ArchitectureTest`: domain purity, floating-point rules |
| FOUND-AC-05 | Web layer | `ProblemDetailsTest` (MockMvc) |
| FOUND-AC-06 | Unit + web layer | `DecimalJsonModuleTest`, `DecimalJsonWebTest` |
| FOUND-AC-07 | Unit + web layer | `DecimalJsonModuleTest`, `DecimalJsonWebTest` |
| FOUND-AC-08 | Frontend | `src/App.test.tsx` |
| FOUND-AC-09 | Tool | `tools/traceability/test/check.test.mjs` with fixture specs and evidence |

## Risks

- **No local Docker on the owner's machine yet.** `HealthIT`, the image builds and the smoke tests can only run in CI until Docker is installed. Mitigation: the CI stack job is the first verification of FOUND-AC-01/02.
- **Error Prone and new JDKs.** Error Prone needs `--add-exports` flags, which are set in `.mvn/jvm.config`. A JDK upgrade may require updating them.
