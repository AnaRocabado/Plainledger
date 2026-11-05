# ADR-0006: Exact decimal arithmetic end to end

- **Status:** Proposed
- **Date:** 2026-11-04

## Context

Quantities can have up to 18 decimals (many tokens use 18), prices range from fractions of a cent to tens of thousands, and fee and cost allocations must sum exactly. Binary floating point (`0.1 + 0.2 ≠ 0.3`) is unacceptable. Precision can also be lost at boundaries we do not control: the database, JSON, and JavaScript `number`.

## Decision

Exact decimals at every layer, with one explicit policy:

| Layer | Rule |
|---|---|
| Database | `NUMERIC(38,18)` for quantities, prices and fees: 20 integer digits, 18 decimals. |
| Input | At most 18 decimals. More is **rejected**, never rounded. |
| Java | `BigDecimal` only. Built from strings or integers, never from `double`. Compared with `compareTo`/`signum`, never `equals` (which is scale-sensitive: `2.0 ≠ 2.00`). |
| Division | Scale 18, `RoundingMode.HALF_EVEN`, through one shared policy class. Addition, subtraction and multiplication stay exact. |
| Allocation | The last share receives the remainder, so the parts always sum to the whole (RULE-09, RULE-10). |
| JSON | Decimals are **strings** in plain notation (`"0.000000010"`, never `1.0E-8`). Requests that send decimals as JSON numbers, or in exponent notation, are rejected with `400`. |
| Frontend | Decimal strings are opaque: formatted for display, never parsed into `number` for arithmetic. |

Guardrails: Error Prone fails the build on `BigDecimalEquals` and `BigDecimalLiteralDouble`. ArchUnit forbids `double`/`float` in `domain` packages and any `BigDecimal` built from a `double`.

## Consequences

- ✅ Results are reproducible to the last digit. Property tests can assert exact equalities (`Σ allocations = total`).
- ✅ The rules are enforced by the build, not by memory.
- ⚠️ More verbose arithmetic code than with primitives.
- ⚠️ API clients must send strings. This is documented in the API and enforced with a clear error.

## Alternatives considered

- **`long` minor units (fixed-point integers)** — fast and exact, but crypto needs up to 18 decimals, and quantity × price overflows 64 bits.
- **`double` with rounding at the end** — errors accumulate and allocations stop summing to the total. Rejected outright.
- **JSON numbers** — JavaScript parses them into `number` and loses precision beyond about 15–17 significant digits.
