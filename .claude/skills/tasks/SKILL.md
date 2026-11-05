---
name: tasks
description: Break an approved Plainledger technical plan into commit-sized tasks (SDD step 3).
argument-hint: <NNN-feature-name>
disable-model-invocation: true
---

# /tasks — break the plan into tasks

Target: `specs/$ARGUMENTS`

## Preconditions

- `spec.md` is `approved` and `plan.md` has been approved by the owner. If unsure, ask.

## Write `tasks.md`

Start from `specs/_templates/tasks.md`.

- Each task is **one commit** that leaves the build green.
- Order tasks so each one builds on the previous: migration → domain → repository → service → controller → frontend → e2e.
- Every task lists the ACs it covers. Every AC in the spec appears in at least one task. Check this before finishing.
- Each task has a concrete "Done when" (which tests pass).
- Keep tasks small: a reviewer should be able to read the diff in under 15 minutes.

## Stop

- Show the task list to the owner and wait for approval.
- Suggested commit: `docs(spec-NNN): add implementation tasks`.
