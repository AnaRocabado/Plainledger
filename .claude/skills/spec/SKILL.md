---
name: spec
description: Draft the spec for a Plainledger feature (SDD step 1). Use when starting a new feature or changing an existing feature's requirements.
argument-hint: <NNN-feature-name> [notes]
disable-model-invocation: true
---

# /spec — draft a feature spec

Target: `specs/$ARGUMENTS`

## Before writing

1. Read `specs/000-product/spec.md` (domain rules, formulas, glossary) and the feature map entry for this feature.
2. Read the ADRs in `docs/adr/` that touch this feature.
3. If the folder already has a `spec.md`, this is a **change**: edit it in place, keep existing AC IDs stable, strike through removed ACs, add a changelog row, and set status back to `draft`.

## Write `spec.md`

Start from `specs/_templates/spec.md`.

- **Context**: the user problem, in the product's language. Reference the `RULE-NN` it builds on instead of restating them.
- **Scope**: be explicit about what is out of scope.
- **Requirements**: `FR-n` / `NFR-n`. Describe behavior, not implementation. No class names, no table names.
- **Acceptance criteria**: `<PREFIX>-AC-NN` in Given/When/Then form. Each one is:
  - observable from outside (API response, UI state, persisted ledger),
  - automatable,
  - specific about numbers. Use exact decimal values and expected results, not "correct PnL".
  - Cover the unhappy paths: validation errors, another user's data, concurrent writes, stale or missing prices.
- **Open questions**: everything you had to guess. Prefer asking over assuming. When the guiding principle ("real numbers, not flattering ones") settles a question, say so and propose that answer.

## Stop

- Set `status: draft`.
- Summarize for the owner: the ACs in one line each, the open questions, and any conflict with the product spec or an ADR.
- **Do not write `plan.md` or any code.** Wait for the owner to approve the spec.
- Suggested commit: `docs(spec-NNN): draft <feature> spec`.
