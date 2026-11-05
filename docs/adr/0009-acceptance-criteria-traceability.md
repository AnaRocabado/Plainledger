# ADR-0009: Acceptance criteria traceability enforced in CI

- **Status:** Proposed
- **Date:** 2026-11-04

## Context

Most of the code is written by an AI agent. The owner's job is to make sure it does what the spec says. "There is a test for that" is easy to claim and hard to check by reading code. We want a mechanical answer to: *is every acceptance criterion verified by a test that actually passed?*

## Decision

1. Every AC has a stable ID (`<PREFIX>-AC-NN`) in its feature spec.
2. Tests declare the ACs they verify:
   - Java: `@Verifies("LEDGER-AC-03")` on the test method or class. A JUnit Platform listener writes the results, with their ACs, to `target/traceability/*.json`. This works for every JUnit engine, including jqwik.
   - TypeScript/Node (Vitest, `node:test`, Playwright): the AC ID in brackets in the test name. These runners emit JUnit XML.
3. `tools/traceability` reads the specs and all result files after the test jobs. It fails CI when:
   - a spec with status `done` has an AC with no **passing** test,
   - a test references an AC that does not exist,
   - an AC ID is duplicated or does not match its spec's prefix,
   - any test referencing an AC failed.
4. It writes the AC → tests matrix to the CI job summary.

## Consequences

- ✅ Coverage of requirements is measured from **test results**, not from the presence of an annotation.
- ✅ Specs cannot silently drift from tests: renaming or removing an AC breaks the build until the tests follow.
- ✅ The matrix gives a reviewer a two-minute view of what is proven.
- ⚠️ A passing test can still be a weak test. The `spec-verifier` agent and human review cover that gap. The check proves presence and success, not quality.
- ⚠️ Small custom tooling to maintain (one script without dependencies, with its own tests).

## Alternatives considered

- **Code coverage thresholds** — they measure executed lines, not verified requirements. We still collect coverage as a signal, but do not gate on it.
- **Grep for AC IDs in test files** — proves that a test mentions the ID, not that it ran or passed.
- **BDD tooling (Cucumber)** — ACs as executable Gherkin. Powerful, but it adds a layer of step definitions for every AC and slows the agent down.
