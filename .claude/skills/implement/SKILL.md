---
name: implement
description: Implement the next task of an approved Plainledger feature, test-first (SDD step 4).
argument-hint: <NNN-feature-name> [task id, e.g. T3]
disable-model-invocation: true
---

# /implement — implement one task

Target: `specs/$ARGUMENTS` (if no task ID is given, take the first unticked task in `tasks.md`).

## Preconditions

- `spec.md` is `approved` or `in-progress`, and `plan.md` and `tasks.md` exist. Otherwise stop.
- On the first task, set the spec status to `in-progress`.

## Loop for one task

1. Re-read the ACs the task covers and the relevant part of `plan.md`.
2. **Tests first**: write the tests for those ACs, annotated with `@Verifies("<AC-ID>")` (Java) or `[<AC-ID>]` in the test name (TypeScript/Node). Run them and confirm they fail for the expected reason.
3. Implement the smallest change that makes them pass. Follow `CLAUDE.md` conventions (layers, BigDecimal policy, user scoping).
4. Run the checks for what you touched:
   - backend: `cd backend && ./mvnw verify`
   - frontend: `cd frontend && npm run check`
5. Tick the task in `tasks.md`.

## Rules

- Implement only what the task's ACs need. If you discover a missing requirement, **stop** and propose a spec change instead of coding it.
- If the plan turns out to be wrong, stop and propose a plan change. Do not drift from the plan silently.
- Never weaken a test, an ArchUnit rule, or a static-analysis check to get to green.

## Stop

- Report: what changed, which ACs are now covered, test results.
- Suggested commit (Conventional Commits), body lists the ACs:
  ```
  feat(<feature>): <what the user can now do>

  Covers: LEDGER-AC-01, LEDGER-AC-02
  ```
- One task per invocation. Wait for the owner before the next task.
