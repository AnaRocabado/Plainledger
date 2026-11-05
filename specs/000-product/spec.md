---
id: "000"
title: Plainledger product spec
status: draft
---

# 000 — Plainledger product spec

## 1. Summary

Plainledger is a multi-user crypto investment **tracker**. Users record every buy and sell they make on any exchange. Plainledger derives honest numbers from that record: what they hold, what each purchase really cost, what they actually gained on what they sold, and where they stand today at market prices.

It is **not** an analysis tool: no trading signals, no technical analysis, no market charts.

## 2. Guiding principle

> **Show the real numbers, not the flattering ones.**

When a rule makes the portfolio look worse but more truthful, we take it. Concretely:

- Fees are always part of the numbers: they raise the cost of a purchase and reduce the gain of a sale.
- Sells realize the cheapest purchases first, so the remaining open position carries its real, more expensive cost (see RULE-05).
- A missing price is shown as unknown, never as zero. An old price is shown with its age.
- We never estimate numbers we do not have, such as a future exit fee.

## 3. Problem

Investors buy at many prices, sell in pieces, and pay fees on both. Exchanges show balances and price history, but not a clear, per-purchase answer to:

1. What do I hold?
2. What did each purchase really cost me, fees included?
3. What have I actually gained or lost on what I sold?
4. Where do I stand today, per purchase, per coin, and in total?

## 4. Users

One persona: a **self-directed retail investor** who buys and sells crypto on one or more exchanges, wants to know their real position, and is willing to enter their operations by hand. Each user sees only their own data.

## 5. Scope

### MVP features

| Spec | Feature | AC prefix | Depends on | Status |
|---|---|---|---|---|
| [001](../001-foundations/spec.md) | Foundations: one-command environment, CI, guardrails | `FOUND` | — | in progress |
| 002 | Accounts: register, log in, log out; all data scoped to the user | `ACCT` | 001 | not started |
| 003 | Transaction ledger: record, edit, delete buys and sells with validation | `LEDGER` | 002 | not started |
| 004 | Purchases and realized PnL: lowest-cost-first matching, purchase list, sell history | `REALIZED` | 003 | not started |
| 005 | Market prices and unrealized PnL: periodic price sync, per purchase / coin / total | `MARKET` | 004 | not started |
| 006 | Operations timeline: chart of the user's own buy and sell prices over time | `TIMELINE` | 003 | not started |

Feature 003 can validate sells without the matching engine: the open quantity at any moment is the sum of buys minus the sum of sells up to that moment, whatever the matching order (RULE-07).

### Out of scope (possible next steps)

Stocks/ETFs and other asset classes · quote currencies other than USDT · fees paid in other assets · exchange CSV/API import · other matching methods (FIFO, HIFO, manual selection) · tax reports · daily portfolio history · alerts · transfers between wallets · Kubernetes/Terraform.

## 6. Glossary

| Term | Definition |
|---|---|
| **Asset** | A supported cryptocurrency, identified by its symbol (e.g. `BTC`). Quoted in USDT. |
| **Transaction** | A user-entered `BUY` or `SELL` of one asset, with an execution time, quantity, unit price and fee. The only financial data that is stored. |
| **Ledger** | All transactions of one user. Per asset, it is that asset's transaction history. |
| **Replay** | Applying an asset's transactions in replay order (RULE-04) to derive its purchases and sell allocations. |
| **Purchase** | The derived record of one `BUY`: original quantity, cost, effective unit cost, remaining (open) quantity and remaining cost. |
| **Open purchase** | A purchase with remaining quantity > 0. |
| **Sell allocation** | The part of a `SELL` that consumed one purchase: consumed quantity, allocated cost, allocated sell fee, proceeds, realized PnL. |
| **Holding** | The open quantity of one asset: sum of the remaining quantities of its open purchases. |
| **Realized PnL** | Gain or loss locked in by sells. |
| **Unrealized PnL** | Gain or loss of open purchases if valued at the current market price. |
| **Market price** | The latest known USDT price of an asset, with the time it was observed. |
| **Stale price** | A market price older than the staleness threshold (defined in spec 005). |
| **USDT** | Tether, the single quote currency in the MVP. All prices, costs, fees and PnL are in USDT. |

## 7. Domain rules

These rules are normative. Feature specs reference them by ID; ACs make them testable.

### Transactions

- **RULE-01 — Transaction shape.** A transaction has: type (`BUY` | `SELL`), asset, execution time (UTC instant, not in the future), quantity (> 0), unit price in USDT (> 0), and fee in USDT (≥ 0). Decimal inputs have at most 18 decimal places. Inputs are stored exactly as entered and never rounded. A value with more decimals is rejected, not truncated.
- **RULE-02 — Fees are in USDT.** If an exchange charges the buy fee in the received coin (for example, buying 1 BTC delivers 0.999 BTC), the user records the **net quantity received** and a fee of 0, so that holdings match the exchange. Converting fees from other assets is out of scope.
- **RULE-03 — The ledger is mutable; derived state is not stored.** Users can create, edit and delete transactions. Purchases, allocations, holdings and PnL are never stored: they are recomputed from the ledger every time they are needed. Consequence: editing or backdating a transaction can change the realized PnL of **past** sells. This is correct, and the UI must not hide it. *(ADR-0004, ADR-0005)*

### Replay and matching

- **RULE-04 — Replay order.** An asset's transactions are applied by execution time ascending. At equal execution time, `BUY`s are applied before `SELL`s. Remaining ties are broken by transaction ID ascending. Replay is therefore deterministic.
- **RULE-05 — Lowest-cost-first matching.** A `SELL` consumes the open purchases that exist at that point of the replay, ordered by **effective unit cost** ascending (RULE-08), then by execution time ascending (oldest first), then by transaction ID ascending. Each purchase is consumed fully before the next one, and the last one may be consumed partially.
  *Why:* realizing the lowest-cost purchases first realizes the biggest gains first and leaves the most expensive purchases open. Unrealized PnL then reflects the real remaining exposure instead of a flattering one. The matching method changes how total PnL splits into realized and unrealized; it never changes the total (see the worked example).
  *Extensibility:* matching sits behind a strategy interface; the MVP has exactly one strategy.
- **RULE-06 — No overselling, ever.** At no point in the replay can a `SELL` exceed the open quantity at that moment. A create, edit or delete that would produce such a history is rejected **as a whole**. The error identifies the first sell that would oversell. Example: deleting a buy that a later sell consumed is rejected until that sell is edited or deleted.
- **RULE-07 — Open quantity is order-independent.** The open quantity at any moment equals the sum of buy quantities minus the sum of sell quantities up to that moment in replay order. RULE-06 can therefore be checked without running the matching.

### Calculations

All formulas use exact decimal arithmetic (RULE-13). For a purchase from a `BUY` with quantity `q`, unit price `p` and fee `f`:

- **RULE-08 — Purchase cost.**
  - `cost = q × p + f`
  - `effectiveUnitCost = cost / q`
- **RULE-09 — Cost allocation.** When a sell consumes quantity `c` of a purchase, the allocated cost is `cost × c / q`. When a consumption takes the **last** remaining quantity of a purchase, its allocated cost is the purchase's remaining cost, so the allocations of a purchase always sum to exactly its cost.
- **RULE-10 — Sell fee allocation.** A `SELL` of quantity `s` with fee `F` that consumes quantities `c₁ … cₙ` (in matching order) allocates `F × cᵢ / s` to each consumption, except the last, which receives `F − Σ(previous allocations)`. The allocated fees always sum to exactly `F`.
- **RULE-11 — Realized PnL.** For each sell allocation with consumed quantity `c` and sell unit price `P`:
  - `proceeds = c × P`
  - `realizedPnl = proceeds − allocatedCost − allocatedSellFee`

  A sell's realized PnL is the sum over its allocations; total realized PnL is the sum over all sells.
- **RULE-12 — Unrealized PnL.** For an open purchase with remaining quantity `r`, remaining cost `rc`, and market price `m`:
  - `marketValue = r × m`
  - `unrealizedPnl = marketValue − rc`
  - `unrealizedPnlPct = unrealizedPnl / rc × 100`

  Per coin:
  - `holding = Σ r`
  - `openCost = Σ rc`
  - `averageCost = openCost / holding`, the weighted average cost of open purchases, fees included
  - `unrealizedPnl = Σ unrealizedPnl`
  - `unrealizedPnlPct = unrealizedPnl / openCost × 100`

  Portfolio totals sum the per-coin values. No estimated exit fee is subtracted. Without a known market price, market value and unrealized PnL are **unavailable** (not zero) for that coin, and portfolio totals state which coins they exclude.
- **RULE-13 — Decimal policy.** Arithmetic is exact `BigDecimal`. Division uses scale 18 with `HALF_EVEN` rounding. Rounding for display happens only in the presentation layer. Decimals cross the API as JSON strings in plain notation. *(ADR-0006)*

### Security and isolation

- **RULE-14 — Data isolation.** Every read and write is scoped to the authenticated user. A resource that belongs to another user behaves exactly like one that does not exist (`404`), so the API never reveals other users' data or its existence.
- **RULE-15 — Concurrent writes are serialized.** Writes to the same user's asset history are serialized, so two concurrent writes can never together produce an oversold history that each would pass alone.

## 8. Worked example

One user, asset BTC. Market price at the end: **48,000** USDT.

| Tx | Executed | Type | Quantity | Unit price | Fee | Cost | Effective unit cost |
|---|---|---|---|---|---|---|---|
| T1 | 2026-01-10 | BUY | 0.5 | 40,000 | 20 | 20,020 | 40,040 |
| T2 | 2026-02-15 | BUY | 0.3 | 30,000 | 9 | 9,009 | 30,030 |
| T3 | 2026-03-20 | BUY | 0.2 | 50,000 | 10 | 10,010 | 50,050 |
| T4 | 2026-04-01 | SELL | 0.6 | 45,000 | 27 | — | — |

**T4 matching** (lowest effective unit cost first: T2 → T1 → T3):

| Consumes | Quantity | Allocated cost | Allocated fee | Proceeds | Realized PnL |
|---|---|---|---|---|---|
| T2 (all) | 0.3 | 9,009 | 13.5 | 13,500 | **4,477.5** |
| T1 (part) | 0.3 | 12,012 | 13.5 (remainder) | 13,500 | **1,474.5** |
| **Total** | 0.6 | 21,021 | 27 | 27,000 | **5,952** |

**Open purchases at 48,000:**

| Purchase | Remaining | Remaining cost | Market value | Unrealized PnL | % |
|---|---|---|---|---|---|
| T1 | 0.2 | 8,008 | 9,600 | 1,592 | 19.88 |
| T3 | 0.2 | 10,010 | 9,600 | −410 | −4.10 |
| **BTC** | 0.4 | 18,018 (avg 45,045) | 19,200 | **1,182** | 6.56 |

**Why lowest-cost-first is the honest choice.** With FIFO, T4 would consume T1 (0.5) and T2 (0.1): realized 3,950 and unrealized 3,184. Both methods total **7,134**, but FIFO shows an open position with 3,184 of paper gains on top of cheap BTC that, in practice, was already sold. Lowest-cost-first shows 1,182: the real cushion of what is left.

## 9. Non-functional requirements

- **NFR-01 — One command.** The whole system starts locally with `make up` on a machine with only Docker installed.
- **NFR-02 — Zero cost.** No paid services. The price provider uses a free public API; nothing else depends on the internet at runtime.
- **NFR-03 — Hermetic tests.** Tests never call external APIs; external HTTP is stubbed (WireMock).
- **NFR-04 — Traceable quality.** Every AC of a `done` spec is verified by at least one passing automated test, enforced in CI.
- **NFR-05 — Responsiveness.** Portfolio views respond in under 300 ms (p95, local environment) for a user with 10,000 transactions. This bounds the compute-on-read approach (ADR-0005) and is measured in spec 005.
- **NFR-06 — Security baseline.** Passwords hashed with a slow adaptive hash. Session cookie `HttpOnly`, `SameSite`, CSRF-protected. No secrets in the repository. *(ADR-0007)*

## 10. Open questions

Resolved in the feature spec named.

- **Q1 (003)** — Supported assets: a fixed, seeded catalog (e.g. top 20 by market cap) or any symbol the price provider knows? *Proposal: fixed seeded catalog.*
- **Q2 (003)** — Can editing a transaction change its asset or type, or must the user delete and re-create it? *Proposal: asset and type are immutable; edit quantity, price, fee and time only.*
- **Q3 (005)** — Price provider and staleness threshold. Binance's public API is USDT-native, but it rejects requests from US IP addresses. Candidates: Kraken, CoinGecko.
- **Q4 (005)** — Portfolio-level percentage: relative to open cost only, or also including realized results?
- **Q5 (006)** — Time zone for display: browser time zone, or a user setting?

## Changelog

| Date | Change |
|---|---|
| 2026-11-04 | Draft from the Phase 0 decisions, plus the matching sort key, tie-breaks, fee remainder rule, fees-in-coin guidance and the worked example. |
