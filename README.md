# Plainledger

A multi-user crypto investment **tracker** that records every buy and sell and derives honest numbers from them: what each purchase really cost, what you actually gained on what you sold, and where you stand today.

> **Show the real numbers, not the flattering ones.**

🚧 **Status: foundations in progress.** No user-facing features yet. See the [feature map](specs/000-product/spec.md#5-scope).

## Run it

Requirements: Docker (with Compose) and `make`.

```bash
make up        # builds and starts everything; open http://localhost:3000
make smoke     # checks the running stack
make down      # stop (make reset also deletes the data)
```

Run `make` to list all targets.

## How this repository works

Plainledger is built with **Spec-Driven Development** and AI agents. Specs are the source of truth; code is derived from them and verified against them.

| Where | What |
|---|---|
| [specs/000-product](specs/000-product/spec.md) | Product spec: domain rules, formulas, worked example |
| [specs/](specs/README.md) | One folder per feature: spec → plan → tasks → verification |
| [docs/adr/](docs/adr/README.md) | Architecture decisions and their trade-offs |
| [docs/architecture.md](docs/architecture.md) | System overview with diagrams |
| [docs/testing-strategy.md](docs/testing-strategy.md) | Test levels, quality gates, acceptance-criteria traceability |
| [CLAUDE.md](CLAUDE.md), [.claude/](.claude/) | Rules and SDD skills for the AI agent |

Every acceptance criterion is referenced by tests, and CI fails when a criterion of a finished feature has no passing test ([ADR-0009](docs/adr/0009-acceptance-criteria-traceability.md)).
