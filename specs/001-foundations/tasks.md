# 001 — Tasks

Plan: [plan.md](plan.md)

Each task is one commit. Tick a task only when its checks are green.

- [x] **T1** — Scaffold the backend: Spring Boot app, static analysis, test infrastructure (`@Verifies`, traceability listener), architecture rules with fixtures
  - Covers: FOUND-AC-03, FOUND-AC-04
  - Done when: `./mvnw verify` passes; ArchitectureTest proves each rule catches its fixture violation
- [x] **T2** — Add the API conventions: Problem Details and the decimal JSON module
  - Covers: FOUND-AC-05, FOUND-AC-06, FOUND-AC-07
  - Done when: web-layer and unit tests pass
- [ ] **T3** — Add the health integration test (Testcontainers) and Actuator configuration
  - Covers: FOUND-AC-02
  - Done when: `HealthIT` passes where Docker is available (CI)
- [x] **T4** — Scaffold the frontend: Vite React TS, Vitest, oxlint, Prettier, app shell
  - Covers: FOUND-AC-08
  - Done when: `npm run check` passes
- [ ] **T5** — Add the traceability check with self-tests
  - Covers: FOUND-AC-09
  - Done when: `node --test tools/traceability` passes; `make traceability` prints the matrix
- [ ] **T6** — Add the container setup: Dockerfiles, Nginx, Compose, Makefile, smoke tests
  - Covers: FOUND-AC-01, FOUND-AC-02
  - Done when: `make up` then `make smoke` pass (CI stack job until Docker is installed locally)
- [ ] **T7** — Add the CI workflows: build/test jobs, stack smoke, traceability gate, image builds, CodeQL, Dependabot
  - Covers: FR-8, FR-9 (no AC)
  - Done when: workflows pass on GitHub
- [ ] **T8** — Verify the feature against its ACs and write `verification.md`
  - Done when: all FOUND ACs show passing tests in the CI traceability report
