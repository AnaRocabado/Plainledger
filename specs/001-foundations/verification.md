# 001 — Verification

Spec: [spec.md](spec.md) · Verified on: 2026-11-04 · Branch: `phase-1/foundations`

**Result: partially verified.** Seven of nine ACs have passing tests locally. FOUND-AC-01 and FOUND-AC-02 need a Docker daemon, which the development machine does not have yet. The spec stays `in-progress` until CI runs them green and the owner accepts the spec.

## Automated checks (local, macOS, Java 21.0.9, Node 24.15)

| Check | Result |
|---|---|
| `./mvnw verify -DskipITs` (Spotless, Error Prone, 44 unit/web/architecture tests) | ✅ |
| `./mvnw verify` integration tests (`HealthIT`) | ❌ not runnable: no Docker daemon (Testcontainers) |
| `make frontend-verify` (oxlint, typecheck, Prettier, Vitest) | ✅ |
| `make tools-test` (16 tests) | ✅ |
| `make up` + `make smoke` | ❌ not runnable: no Docker daemon |
| Traceability (`make traceability`) | 7 / 9 covered; the check fails on FOUND-AC-02, as designed |
| CI on GitHub | ⏳ not run yet (branch not pushed) |

## Guardrails proven to fail

A guardrail that never fails proves nothing. Each one was triggered on purpose:

| Guardrail | How it was triggered | Result |
|---|---|---|
| Error Prone `BigDecimalEquals`, `BigDecimalLiteralDouble` | Temporary class calling `a.equals(b)` and `new BigDecimal(0.1)` | Build failed with both errors |
| ArchUnit rules (5) | Fixture packages under `architecture/fixtures`, one per violation | Each rule reports its fixture; the compliant fixture passes all rules |
| Decimal JSON module | Removed its `@Component` registration | All 4 web tests failed |
| Traceability check | Ran on a machine where `HealthIT` could not start | Exit code 1, naming the failed test |
| TypeScript strict | Temporary `const x: number = "a"` | `tsc -b` failed |

## Acceptance criteria

| AC | Evidence (tests) | Review |
|---|---|---|
| FOUND-AC-01 | `tools/smoke/smoke.test.mjs`: root URL and `/portfolio` serve the SPA | ⏳ not run (needs Docker) |
| FOUND-AC-02 | `HealthIT` (Testcontainers); smoke: health through Nginx with `db` `UP` | ⏳ not run (needs Docker) |
| FOUND-AC-03 | `ArchitectureTest`: production code, compliant fixture, layering and isolation fixtures | ✅ each rule is shown to fail on its violation |
| FOUND-AC-04 | `ArchitectureTest`: purity, floating-point fields/params/returns, `BigDecimal` from `double` (3 forms) | ✅ gap found in review and fixed: the `(double, MathContext)` constructor had no fixture |
| FOUND-AC-05 | `ProblemDetailsTest` (MockMvc); `DecimalJsonWebTest` (400 is Problem Details); smoke through Nginx | ✅ asserts status, content type, and the absence of stack traces |
| FOUND-AC-06 | `DecimalJsonModuleTest` (7 values incl. the 4 in the AC, null); `DecimalJsonWebTest` through Spring MVC | ✅ exact string comparison |
| FOUND-AC-07 | `DecimalJsonModuleTest` (accepts 5, rejects 4 numbers + 9 malformed strings + boolean); `DecimalJsonWebTest` | ✅ "exactly" checked via digits and scale of the parsed value |
| FOUND-AC-08 | `src/App.test.tsx` | ✅ queries by role and exact text |
| FOUND-AC-09 | `check.test.mjs` (16, incl. CLI end to end); `TraceabilityListenerTest` (status mapping, `@Nested` inheritance, no output without a suite) | ✅ every failure condition in the AC has its own test |

## Review notes

This review was done by the implementing agent, not by the independent `spec-verifier` agent. That agent is available for Phase 2 features (`/verify`).

- **Stale evidence.** `make traceability` reads whatever result files exist locally. A file from an older run can make it report a stale result: here, the `HealthIT` failure from a run without Docker. CI always starts from clean result files, so the CI gate is authoritative. `make clean` resets local results.
- **Smoke tests cover the assembled system only.** Nginx routing rules (`/api/`, exact `/actuator/health`, SPA fallback) have no unit test. The smoke tests exercise all three through the real containers.

## Pending before `done`

1. Install a Docker runtime locally (Docker Desktop, OrbStack or Colima), then run `make verify`, `make up` and `make smoke`, **or** push the branch and let CI run them.
2. Owner review of `spec.md` (it was implemented under the Phase 1 go-ahead, without a separate approval gate).
3. Set `status: done` in `spec.md`. From then on CI fails if any FOUND AC loses its passing test.
