---
name: plan
description: Write the technical plan for an approved Plainledger feature spec (SDD step 2).
argument-hint: <NNN-feature-name>
disable-model-invocation: true
---

# /plan — technical plan for an approved spec

Target: `specs/$ARGUMENTS`

## Preconditions

- `spec.md` exists and its front matter says `status: approved`. If not, stop and tell the owner. Do not plan a draft.
- The spec has no open questions left.

## Research

1. Read the spec, the product spec rules it references, and all `Accepted` ADRs.
2. Read the existing code the feature touches. Reuse what exists before adding new abstractions.

## Write `plan.md`

Start from `specs/_templates/plan.md`.

- Follow the package rules in `CLAUDE.md`. Put calculation logic in the feature's `domain` package as pure functions.
- **Design**: API contract (paths, shapes, status codes, Problem Details types), schema changes as a named Flyway migration, domain types and the invariants they guarantee, frontend changes.
- **Decisions table**: every non-obvious choice with the alternatives considered and why. Prefer the simplest design that meets the ACs. Do not design for requirements the spec does not have.
- If a decision outlives this feature or changes an existing ADR, list a new ADR to write (`/adr`). Ask the owner before any architecture change.
- **Test strategy**: map **every** AC to a test level and a concrete test name. Pick the cheapest level that proves the behavior:
  - domain rules → unit + jqwik property tests,
  - persistence, locking, data isolation → Testcontainers `*IT`,
  - HTTP contract and validation → web layer tests,
  - critical user journeys → one Playwright test.
- **Risks**: what could go wrong and how the tests catch it.

## Stop

- Present the plan summary and the decisions table to the owner.
- **Do not write tasks or code** until the owner approves the plan.
- Suggested commit: `docs(spec-NNN): add technical plan`.
