# NNN — Verification

Spec: [spec.md](spec.md) · Verified on: YYYY-MM-DD · Commit: `<sha>`

## Automated checks

| Check | Result |
|---|---|
| `make verify` | ✅ / ❌ |
| Traceability (`make traceability`) | N/N ACs covered by passing tests |

## Acceptance criteria

| AC | Evidence (tests) | Independent review |
|---|---|---|
| PREFIX-AC-01 | `ClassTest#method` | ✅ asserts the behavior / ⚠️ weak: ... |

## Findings from the independent review

Output of the `spec-verifier` agent: weak tests, untested edge cases, and how each was resolved.

## Manual checks

Anything checked by hand (for example, the UI in a browser), with the result.
