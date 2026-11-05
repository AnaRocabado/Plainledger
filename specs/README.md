# Specs

Specs are the source of truth for Plainledger. Code is derived from them and verified against them.

## Layout

```
specs/
  000-product/spec.md          Product spec: scope, domain rules (RULE-NN), formulas, glossary
  NNN-<feature>/
    spec.md                    What and why: context, requirements, acceptance criteria
    plan.md                    How: design, API, data model, test strategy per AC
    tasks.md                   Ordered, commit-sized tasks
    verification.md            Evidence that every AC is met
  _templates/                  Templates for the files above
```

## Lifecycle

```mermaid
stateDiagram-v2
    direction LR
    [*] --> draft: /spec
    draft --> approved: owner review
    approved --> in-progress: plan + tasks approved, /implement
    in-progress --> done: /verify, owner accepts
    done --> in-progress: spec change
```

The status lives in the `status:` field of each `spec.md` front matter.

| Status | Meaning | CI traceability check |
|---|---|---|
| `draft` | Written, waiting for the owner's review | Reported only |
| `approved` | Owner approved the requirements and ACs | Reported only |
| `in-progress` | Being implemented, task by task | Reported only |
| `done` | All ACs verified | **Fails the build** if any AC lacks a passing test |

Some checks always fail the build regardless of status: a test that references an unknown AC, a duplicate AC ID, or an AC whose prefix does not match its spec.

## Acceptance criteria

- Format: one list item per criterion, ID in bold, written as observable behavior:
  `- **LEDGER-AC-03** — Given a holding of 1 BTC, when I record a sell of 1.5 BTC, then it is rejected with ...`
- IDs are `<PREFIX>-AC-<NN>`. The prefix is declared in the spec's front matter. IDs are never reused or renumbered; a removed AC is struck through and left in place.
- Each AC must be checkable by an automated test. If something can only be checked by a person, it is a requirement, not an AC.

## Traceability

Tests declare which ACs they verify (`@Verifies` in Java, `[ID]` in the test name in TypeScript/Node). After the test suites run, `tools/traceability` reads the specs and the test result files and produces a matrix of AC → passing tests. See [ADR-0009](../docs/adr/0009-acceptance-criteria-traceability.md).

```bash
make traceability
```
