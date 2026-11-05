---
name: adr
description: Write a new Architecture Decision Record for Plainledger.
argument-hint: <short decision title>
disable-model-invocation: true
---

# /adr — record an architecture decision

Decision: $ARGUMENTS

1. Find the next number in `docs/adr/` and create `docs/adr/NNNN-<kebab-title>.md` from `docs/adr/template.md`.
2. Keep it short (one screen). Context: the forces at play. Decision: one clear sentence first, then details. Consequences: good **and** bad. Alternatives: each with the reason it lost.
3. Status is `Proposed` until the owner accepts it. If it replaces an older ADR, set the old one to `Superseded by ADR-NNNN` and link both ways.
4. Add it to the index in `docs/adr/README.md`.
5. Suggested commit: `docs(adr): add ADR-NNNN <title>`.
