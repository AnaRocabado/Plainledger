# ADR-0004: The transaction ledger is mutable; history is validated by replay

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

Users make data-entry mistakes and enter operations late. They must be able to edit, delete and backdate transactions. Any of these can change the past: a deleted buy can leave a later sell without enough coins.

## Decision

Transactions are stored in a **mutable** table: edit is an update, delete is a delete. Every write is validated by **replaying the asset's full history with the change applied**. The write is accepted only if no sell ever exceeds the open quantity at its moment (RULE-06). Derived state is never stored (ADR-0005), so nothing else needs to be updated.

## Consequences

- ✅ Simple data model and queries: the current ledger is the table.
- ✅ The validation rule is one function over a list of transactions, easy to unit- and property-test.
- ✅ Backdating is supported: the replay shows its real effect, including on past realized PnL.
- ⚠️ No audit trail: after an edit, the previous values are gone. Acceptable for a personal tracker in the MVP.
- ⚠️ Validation cost grows with the size of an asset's history (bounded by NFR-05).

## Alternatives considered

- **Append-only ledger with revisions and tombstones** — full audit trail, but every query must filter for the current revision, and it adds about 2–3 days of work. A good next step if auditability becomes a requirement.
- **Event sourcing** — same benefit, much higher complexity than the problem needs.
- **Forbid editing and deleting** — simpler, but users would keep wrong data or work around it.
