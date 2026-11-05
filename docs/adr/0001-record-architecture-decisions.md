# ADR-0001: Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-11-04

## Context

Plainledger is built with AI agents following Spec-Driven Development. Agents need durable, explicit constraints, or they re-decide the same questions in every session. Reviewers need to see *why* the system looks the way it does, not only *what* it is.

## Decision

We record every decision that has trade-offs and outlives a single feature as a short ADR in `docs/adr/`, numbered sequentially and never deleted. A changed decision gets a new ADR that supersedes the old one.

## Consequences

- ✅ Agents and humans share the same constraints; `CLAUDE.md` points to the ADRs instead of repeating them.
- ✅ The reasoning stays reviewable in interviews and code reviews.
- ⚠️ A small writing cost per decision. Keeping ADRs to one screen keeps that cost low.

## Alternatives considered

- **Decisions only in commit messages or PR descriptions** — hard to find later, and agents do not read them.
- **A single design document** — goes stale. It also mixes current state with history.
