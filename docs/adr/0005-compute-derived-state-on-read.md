# ADR-0005: Derived state is computed on every read, never stored

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

Purchases, sell allocations, holdings and realized PnL are pure functions of an asset's transaction history (RULE-03..12). The ledger is mutable and supports backdating (ADR-0004), so any write can change any derived value of that asset, including past ones.

## Decision

We store **only transactions**. On each read, the backend loads the user's transactions for the relevant assets and replays them in the framework-free domain engine to produce purchases, allocations and PnL. The same replay validates writes (ADR-0004).

## Consequences

- ✅ One source of truth. Derived data can never be stale or inconsistent, because it does not exist at rest.
- ✅ Edits, deletes and backdating need no extra code paths: the next read is simply correct.
- ✅ The engine is a pure function, the ideal target for property-based tests.
- ⚠️ CPU work on every read. A personal ledger is small (hundreds to a few thousand transactions per asset), and replay is linear in the history plus a sort. NFR-05 (300 ms p95 at 10,000 transactions) is the budget, measured in spec 005.
- ⚠️ Sorting and paging of derived lists (e.g. purchases by price) happen in memory, not in SQL.

**Scaling path if NFR-05 is ever missed:** cache the replay result per (user, asset), invalidated on writes. If that is not enough, persist snapshots. Neither is needed at MVP scale.

## Alternatives considered

- **Persisted projections** (`purchase`, `sell_allocation` tables rebuilt in the same DB transaction as each write) — SQL sorting and paging, but two sources of truth to keep consistent. Every bug becomes a data-repair problem.
- **Incremental updates of stored balances** — fast, but backdating and edits require reverse-and-reapply logic, which is error-prone.
