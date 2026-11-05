---
name: verify
description: Verify a Plainledger feature against its acceptance criteria and write the verification report (SDD step 5).
argument-hint: <NNN-feature-name>
disable-model-invocation: true
---

# /verify — verify a feature against its acceptance criteria

Target: `specs/$ARGUMENTS`

## 1. Automated evidence

Run, from the repo root:

```bash
make verify          # format, static analysis, all test suites
make traceability    # AC -> passing tests matrix
```

Every AC of this spec must show at least one passing test. If not, go back to `/implement`. Do not mark the feature done.

## 2. Independent review

Delegate to the `spec-verifier` agent with the spec path. It reviews the tests **as a skeptic**: does each test actually assert the behavior its AC describes, or does it only mention the ID? Treat its findings as blocking unless the owner says otherwise. Fix weak tests before continuing.

## 3. Write `verification.md`

Start from `specs/_templates/verification.md`: automated check results, the AC → test table with the reviewer's verdict per AC, findings and how each was resolved, and any manual checks.

## 4. Close

- Set `status: done` in `spec.md`. From now on CI fails if any of its ACs loses its passing test.
- Update `docs/architecture.md` and the feature map in the product spec if anything changed.
- Prepare the owner summary: key technical decisions with alternatives considered, and 3–5 interview questions about this feature with key points for answering.
- Suggested commit: `docs(spec-NNN): verify <feature> against acceptance criteria`.
